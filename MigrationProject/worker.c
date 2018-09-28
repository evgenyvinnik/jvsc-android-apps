/* Copyright (c) 2012. MRSG Team. All rights reserved. */

/* This file is part of MRSG.

 MRSG is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 MRSG is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with MRSG.  If not, see <http://www.gnu.org/licenses/>. */

#include "common.h"
#include "dfs.h"

XBT_LOG_EXTERNAL_DEFAULT_CATEGORY(msg_test);

static void heartbeat(int configuration_id);
static int listen(int argc, char* argv[]);
static int compute(int argc, char* argv[]);
static void update_map_output(msg_process_t worker, size_t mid, int configuration_id);
static void get_chunk(msg_process_t worker, task_info_t ti, int configuration_id);
static void get_map_output(msg_process_t worker, task_info_t ti, int configuration_id);
static void my_ram_operations_function(enum phase_e phase, size_t tid, size_t wid, int configuration_id, long unsigned int fraction);
static void my_disk_operations_function(enum phase_e phase, size_t tid, size_t wid, int configuration_id, long unsigned int fraction);

#define IO_FRACTION 1024

/**
 * @brief  Main worker function.
 *
 * This is the initial function of a worker node.
 * It creates other processes and runs a heartbeat loop.
 */
int worker(int argc, char* argv[])
{
	char mailbox[MAILBOX_ALIAS_SIZE];
	char sms_finish[MAILBOX_ALIAS_SIZE];

	msg_process_t my_process, process_create, process_data_node;
	msg_host_t my_host;
	msg_vm_t vm;
	int my_id, config_id;
	vm = (msg_vm_t) MSG_process_get_data(MSG_process_self());

	my_host = MSG_host_self();
	my_process = MSG_process_self();
	xbt_assert(argc >= 2, "worker function requires at least 2 arguments - its ID and config ID");
	sscanf(argv[0], "%d", &my_id);
	sscanf(argv[1], "%d", &config_id);

	MSG_process_set_data(my_process, (void*) my_id);
	//my_id = MSG_process_self_PID();
	/* Spawn a process that listens for tasks. */
	char**argv_create = xbt_new(char*,2);
	argv_create[0] = bprintf("%d", config_id);
	argv_create[1] = NULL;
	process_create = MSG_process_create_with_arguments("listen", listen, vm, my_host, 1, argv_create);
	MSG_vm_bind(vm, process_create);

	/* Spawn a process to exchange data with other workers. */
	char**argv_data_node = xbt_new(char*,2);
	argv_data_node[0] = bprintf("%d", config_id);
	argv_data_node[1] = NULL;
	process_data_node = MSG_process_create_with_arguments("data-node", data_node, vm, my_host, 1, argv_data_node);
	MSG_vm_bind(vm, process_data_node);
	/* Start sending heartbeat signals to the master node. */
	heartbeat(config_id);

	sprintf(sms_finish, SMS_FINISH, config_id);
	sprintf(mailbox, DATANODE_MAILBOX, config_id, get_worker_id(my_process));
	send_sms(sms_finish, mailbox); //TODO might be dangerous
	sprintf(mailbox, TASKTRACKER_MAILBOX, config_id, get_worker_id(my_process));
	send_sms(sms_finish, mailbox); //TODO might be dangerous

	return 0;
}

/**
 * @brief  The heartbeat loop.
 */
static void heartbeat(int configuration_id)
{
	char master_mailbox[MAILBOX_ALIAS_SIZE];
	char sms_heartbeat[MAILBOX_ALIAS_SIZE];

	sprintf(master_mailbox, MASTER_MAILBOX, configuration_id);
	sprintf(sms_heartbeat, SMS_HEARTBEAT, configuration_id);

	while (!jobs[configuration_id].finished)
	{
		send_sms(sms_heartbeat, master_mailbox);
		MSG_process_sleep(configs[configuration_id].heartbeat_interval);
	}
}

/**
 * @brief  Process that listens for tasks.
 */
static int listen(int argc, char* argv[])
{
	char mailbox[MAILBOX_ALIAS_SIZE];
	char sms_task[MAILBOX_ALIAS_SIZE];
	char sms_finish[MAILBOX_ALIAS_SIZE];
	int parent_id;
	msg_process_t parent_process, process_compute;
	msg_host_t my_host;
	msg_task_t msg = NULL;
	msg_vm_t vm;
	int config_id;

	xbt_assert(argc >= 1, "listen function requires at least 1 argument - the config ID");
	sscanf(argv[0], "%d", &config_id);

	vm = (msg_vm_t) MSG_process_get_data(MSG_process_self());
	//const char* process_name = NULL;

	my_host = MSG_host_self();

	parent_id = MSG_process_self_PPID();
	parent_process = MSG_process_from_PID(parent_id);
	//process_name = MSG_process_get_name(parent_process);

	sprintf(mailbox, TASKTRACKER_MAILBOX, config_id, get_worker_id(parent_process));
	sprintf(sms_task, SMS_TASK, config_id);
	sprintf(sms_finish, SMS_FINISH, config_id);

	while (!jobs[config_id].finished)
	{
		msg = NULL;
		receive(&msg, mailbox);

		if (message_is(msg, sms_task))
		{
			char**argv_compute = xbt_new(char*,2);
			argv_compute[0] = bprintf("%d", config_id);
			argv_compute[1] = NULL;
			process_compute = MSG_process_create_with_arguments("compute", compute, msg, my_host, 1, argv_compute);
			MSG_vm_bind(vm, process_compute);
		}
		else if (message_is(msg, sms_finish))
		{
			MSG_task_destroy(msg);
			break;
		}
	}

	return 0;
}

/**
 * @brief  Process that computes a task.
 */
static int compute(int argc, char* argv[])
{
	char task_done[MAILBOX_ALIAS_SIZE];
	char master_mailbox[MAILBOX_ALIAS_SIZE];
	msg_error_t error;
	msg_task_t task;
	task_info_t ti;
	xbt_ex_t e;
	int grand_parent_id;
	msg_process_t grand_parent_process_worker;
	int parent_id;
	msg_process_t parent_process;
	size_t wid;
	int config_id;
	long unsigned int fraction;

	xbt_assert(argc >= 1, "compute function requires at least 1 argument - the config ID");
	sscanf(argv[0], "%d", &config_id);

	parent_id = MSG_process_self_PPID();
	parent_process = MSG_process_from_PID(parent_id);
	grand_parent_id = MSG_process_get_PPID(parent_process);
	grand_parent_process_worker = MSG_process_from_PID(grand_parent_id);
	wid = get_worker_id(grand_parent_process_worker);

	task = (msg_task_t) MSG_process_get_data(MSG_process_self());
	ti = (task_info_t) MSG_task_get_data(task);
	ti->pid = MSG_process_self_PID();
	ti->worker_process = grand_parent_process_worker;

	switch (ti->phase)
	{
	case MAP:
		get_chunk(grand_parent_process_worker, ti, config_id);
		XBT_INFO("\t\tconfig %d map %zu received by task tracker\t wid %d\t on %s", config_id, ti->id, wid,
		        MSG_host_get_name(MSG_process_get_host(grand_parent_process_worker)));

		break;

	case REDUCE:
		get_map_output(grand_parent_process_worker, ti, config_id);
		XBT_INFO("\t\tconfig %d reduce %zu received by task tracker\t wid %d\t on %s", config_id, ti->id, wid,
		        MSG_host_get_name(MSG_process_get_host(grand_parent_process_worker)));
		break;
	}

	if (jobs[config_id].task_status[ti->phase][ti->id] != T_STATUS_DONE)
	{
		TRY
				{
					//perform execution (CPU)
					error = MSG_task_execute(task);
					
					//NOTE: important thing: we split entire task in 1024 pieces
					//so the contention would be somewhat more realistic
					for (fraction = 0; fraction < IO_FRACTION; fraction++)
					{

						//perform memory operations
						my_ram_operations_function(ti->phase, ti->id, ti->wid, config_id, IO_FRACTION);
						//perform disk operations

						my_disk_operations_function(ti->phase, ti->id, ti->wid, config_id, IO_FRACTION);
					}

					if (ti->phase == MAP && error == MSG_OK)
						update_map_output(grand_parent_process_worker, ti->id, config_id);
				}
					CATCH(e)
		{
			xbt_assert(e.category == cancel_error, "%s", e.msg);
			xbt_ex_free(e);
		}
	}

	w_heartbeats[config_id][ti->wid].slots_av[ti->phase]++;

	if (!jobs[config_id].finished)
	{
		sprintf(task_done, SMS_TASK_DONE, config_id);
		sprintf(master_mailbox, MASTER_MAILBOX, config_id);
		send(task_done, 0.0, 0.0, ti, master_mailbox);
	}

	switch (ti->phase)
	{
	case MAP:
		XBT_INFO("\t\tconfig %d map %zu complete by task tracker\t wid %d\t on %s", config_id, ti->id, wid,
		        MSG_host_get_name(MSG_process_get_host(grand_parent_process_worker)));

		break;

	case REDUCE:
		XBT_INFO("\t\tconfig %d reduce %zu complete by task tracker\t wid %d\t on %s", config_id, ti->id, wid,
		        MSG_host_get_name(MSG_process_get_host(grand_parent_process_worker)));
		break;
	}

	return 0;
}

static void my_ram_operations_function(enum phase_e phase, size_t tid, size_t wid, int configuration_id, long unsigned int fraction)
{
	msg_file_t file = NULL;
	void *ptr = NULL;
	double read;

	file = MSG_file_open("/slot", "./memory/mem.mem", "rw");

#ifdef VERBOSE
		XBT_INFO("\tRam start reading");
#endif

	switch (phase)
	{
	case MAP:
		read = MSG_file_read(ptr, (size_t) (configs[configuration_id].ram_operations_map / fraction), sizeof(char*), file);

		break;
	case REDUCE:
		read = MSG_file_read(ptr, (size_t) (configs[configuration_id].ram_operations_reduce / fraction), sizeof(char*), file);

		break;
	}

#ifdef VERBOSE
	XBT_INFO("\tHave read    %8.1f on %s", read, file->name);
	s_msg_stat_t stat;
	MSG_file_stat(file, &stat);
	XBT_INFO("\tFile stat %s Size %.1f", file->name, stat.size);
	MSG_file_free_stat(&stat);
#endif

	MSG_file_close(file);

}

static void my_disk_operations_function(enum phase_e phase, size_t tid, size_t wid, int configuration_id, long unsigned int fraction)
{
	msg_file_t file = NULL;
	void *ptr = NULL;
	double read;

	file = MSG_file_open("/home", "./disk/disk.disk", "rw");

#ifdef VERBOSE
		XBT_INFO("\tDisk start reading");
#endif

		switch (phase)
	{
	case MAP:
		read = MSG_file_read(ptr, (size_t) (configs[configuration_id].disk_operations_map / fraction), sizeof(char*), file);

		break;
	case REDUCE:
		read = MSG_file_read(ptr, (size_t) (configs[configuration_id].disk_operations_reduce / fraction), sizeof(char*), file);

		break;
	}

#ifdef VERBOSE
	XBT_INFO("\tHave read    %8.1f on %s", read, file->name);
	s_msg_stat_t stat;
	MSG_file_stat(file, &stat);
	XBT_INFO("\tFile stat %s Size %.1f", file->name, stat.size);
	MSG_file_free_stat(&stat);
#endif

	MSG_file_close(file);

}

/**
 * @brief  Update the amount of data produced by a mapper.
 * @param  worker  The worker that finished a map task.
 * @param  mid     The ID of map task.
 */
static void update_map_output(msg_process_t worker, size_t mid, int configuration_id)
{
	size_t rid;
	size_t wid;

	wid = get_worker_id(worker);

	for (rid = 0; rid < configs[configuration_id].number_of_reduces; rid++)
		jobs[configuration_id].map_output[wid][rid] += user.map_output_f(mid, rid, configuration_id);
}

/**
 * @brief  Get the chunk associated to a map task.
 * @param  ti  The task information.
 */
static void get_chunk(msg_process_t worker, task_info_t ti, int configuration_id)
{
	char mailbox[MAILBOX_ALIAS_SIZE];
	char get_chunk[MAILBOX_ALIAS_SIZE];

	msg_task_t data = NULL;
	size_t my_id;

	my_id = get_worker_id(worker);

	/* Request the chunk to the source node if we are performing it remotely */
	if (ti->src != my_id)
	{
		sprintf(mailbox, DATANODE_MAILBOX, configuration_id, ti->src);
		sprintf(get_chunk, SMS_GET_CHUNK, configuration_id);
		send(get_chunk, 0.0, 0.0, ti, mailbox);					//get_chunk message would take care of disk access

		sprintf(mailbox, TASK_MAILBOX, configuration_id, my_id, MSG_process_self_PID());
		receive(&data, mailbox);

		MSG_task_destroy(data);
	}
	else //otherwise just simulate disk access
	{

		msg_file_t file = NULL;
		void *ptr = NULL;
		double read;

		file = MSG_file_open("/home", "./disk/disk.disk", "rw");
#ifdef VERBOSE
		XBT_INFO("\tDFS start reading");
#endif

		read = MSG_file_read(ptr, (size_t) configs[configuration_id].chunk_size, sizeof(char*), file);     // Read for 10Mo

#ifdef VERBOSE
		XBT_INFO("\tDFS read    %8.1f on %s", read, file->name);
		s_msg_stat_t stat;
		MSG_file_stat(file, &stat);
		XBT_INFO("\tFile stat %s Size %.1f", file->name, stat.size);
		MSG_file_free_stat(&stat);
#endif
		MSG_file_close(file);

	}
}

/**
 * @brief  Copy the itermediary pairs for a reduce task.
 * @param  ti  The task information.
 */
static void get_map_output(msg_process_t worker, task_info_t ti, int configuration_id)
{
	char mailbox[MAILBOX_ALIAS_SIZE];
	char ger_inter_pairs[MAILBOX_ALIAS_SIZE];
	msg_task_t data = NULL;
	unsigned long long total_copied, must_copy;
	size_t mid;
	size_t my_id;
	size_t wid;
	unsigned long long* data_copied;

#ifdef VERBOSE
	msg_host_t dest_host = MSG_process_get_host(worker);
#endif

	my_id = get_worker_id(worker);
	data_copied = xbt_new0 (unsigned long long, configs[configuration_id].number_of_workers);
	ti->map_output_copied = data_copied;
	total_copied = 0;
	must_copy = 0;
	for (mid = 0; mid < configs[configuration_id].number_of_maps; mid++)
		must_copy += user.map_output_f(mid, ti->id, configuration_id);

#ifdef VERBOSE
	XBT_INFO("INFO: config %d start copy must_copy %llu, reduce %zu, task tracker\t wid %zu\t on %s", configuration_id, must_copy, ti->id, my_id,
			MSG_host_get_name(dest_host));
#endif

	while (total_copied < must_copy)
	{
		for (wid = 0; wid < configs[configuration_id].number_of_workers; wid++)
		{
			if (jobs[configuration_id].task_status[REDUCE][ti->id] == T_STATUS_DONE)
			{
				xbt_free_ref(&data_copied);
				return;
			}

			if (jobs[configuration_id].map_output[wid][ti->id] > data_copied[wid])
			{
				sprintf(mailbox, DATANODE_MAILBOX, configuration_id, wid);
				sprintf(ger_inter_pairs, SMS_GET_INTER_PAIRS, configuration_id);
				send(ger_inter_pairs, 0.0, 0.0, ti, mailbox);

				sprintf(mailbox, TASK_MAILBOX, configuration_id, my_id, MSG_process_self_PID());
				data = NULL;
				receive(&data, mailbox);
				if(MSG_task_get_data_size(data) > 0)
				{
					data_copied[wid] += (unsigned long long) MSG_task_get_data_size(data);
					total_copied += (unsigned long long) MSG_task_get_data_size(data);
				}
				else
				{
					data_copied[wid] += (unsigned long long) MSG_task_get_compute_duration(data);
					total_copied += (unsigned long long) MSG_task_get_compute_duration(data);
				}
				MSG_task_destroy(data);
			}
		}
		/* (Hadoop 0.20.2) mapred/ReduceTask.java:1979 */
		MSG_process_sleep(5);
	}

#ifdef VERBOSE
	XBT_INFO("INFO: config %d  copy finished. received %llu, reduce %zu, task tracker\t wid %zu\t on %s", configuration_id, total_copied, ti->id, my_id,
			MSG_host_get_name(dest_host));
#endif
	ti->shuffle_end = MSG_get_clock();

	xbt_free_ref(&data_copied);
}

