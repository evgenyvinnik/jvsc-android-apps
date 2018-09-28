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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "common.h"
#include "dfs.h"

XBT_LOG_EXTERNAL_DEFAULT_CATEGORY(msg_test);



static void print_config(int configuration_id);
static void print_stats(int configuration_id);
static int is_straggler(msg_process_t worker, int configuration_id);
static double task_time_elapsed(msg_task_t task);
static void set_speculative_tasks(msg_process_t worker, int configuration_id);
static void send_map_to_worker(msg_process_t dest, int configuration_id, FILE* tasks_log);
static void send_reduce_to_worker(msg_process_t dest, int configuration_id, FILE* tasks_log);
static void send_task(enum phase_e phase, size_t tid, size_t data_src, msg_process_t dest, int configuration_id, FILE* tasks_log);
static void finish_all_task_copies(task_info_t ti, int configuration_id, FILE* tasks_log);

/** @brief  Main master function. */
int master(int argc, char* argv[])
{
	char master_mailbox[MAILBOX_ALIAS_SIZE];
	char sms_heartbeat[MAILBOX_ALIAS_SIZE];
	char sms_task_done[MAILBOX_ALIAS_SIZE];
	char tasks_log_name[MAILBOX_ALIAS_SIZE];
	FILE* tasks_log;

	heartbeat_t heartbeat;
	msg_process_t worker;
	msg_task_t msg = NULL;
	size_t wid;
	task_info_t ti;
	int config_id;

	xbt_assert(argc >= 1, "master function requires at least 1 argument - its config ID");
	sscanf(argv[0], "%d", &config_id);

	sprintf(master_mailbox, MASTER_MAILBOX, config_id);
	sprintf(sms_heartbeat, SMS_HEARTBEAT, config_id);
	sprintf(sms_task_done, SMS_TASK_DONE, config_id);
	sprintf(tasks_log_name, "tasks%d.log", config_id);

	print_config(config_id);
	XBT_INFO("JOB BEGIN master config %d", config_id);
	XBT_INFO(" ");

	tasks_log = fopen(tasks_log_name, "w");

	while (jobs[config_id].tasks_pending[MAP] + jobs[config_id].tasks_pending[REDUCE] > 0)
	{
		msg = NULL;
		receive(&msg, master_mailbox);
		ti = (task_info_t) MSG_task_get_data(msg);
		if (ti == NULL )
		{
			worker = MSG_task_get_sender(msg);
		}
		else
		{
			worker = ti->worker_process;
		}
		wid = get_worker_id(worker);

		if (message_is(msg, sms_heartbeat))
		{
			heartbeat = &w_heartbeats[config_id][wid];

			if (is_straggler(worker, config_id))
			{
				set_speculative_tasks(worker, config_id);
			}
			else
			{
				if (heartbeat->slots_av[MAP] > 0)
					send_map_to_worker(worker, config_id, tasks_log);

				if (heartbeat->slots_av[REDUCE] > 0)
					send_reduce_to_worker(worker, config_id, tasks_log);
			}
		}
		else if (message_is(msg, sms_task_done))
		{

			switch (ti->phase)
			{
			case MAP:
				if (jobs[config_id].task_status[MAP][ti->id] != T_STATUS_DONE)
				{
					jobs[config_id].task_status[MAP][ti->id] = T_STATUS_DONE;
					finish_all_task_copies(ti, config_id, tasks_log);
					statistics[config_id].maps_processed[wid]++;
					jobs[config_id].tasks_pending[MAP]--;
					if (jobs[config_id].tasks_pending[MAP] <= 0)
					{
						XBT_INFO(" ");
						XBT_INFO("MAP PHASE DONE config %d", config_id);
						XBT_INFO(" ");
					}
				}
				break;

			case REDUCE:
				if (jobs[config_id].task_status[REDUCE][ti->id] != T_STATUS_DONE)
				{
					jobs[config_id].task_status[REDUCE][ti->id] = T_STATUS_DONE;
					finish_all_task_copies(ti, config_id, tasks_log);
					statistics[config_id].reduces_processed[wid]++;
					jobs[config_id].tasks_pending[REDUCE]--;
					if (jobs[config_id].tasks_pending[REDUCE] <= 0)
					{
						XBT_INFO(" ");
						XBT_INFO("REDUCE PHASE DONE config %d", config_id);
						XBT_INFO(" ");
					}
				}
				break;
			}
			xbt_free_ref(&ti);
		}
		MSG_task_destroy(msg);
	}

	fclose(tasks_log);

	jobs[config_id].finished = 1;

	print_config(config_id);
	print_stats(config_id);
	XBT_INFO("JOB END config %d", config_id);

	return 0;
}

/** @brief  Print the job configuration. */
static void print_config(int configuration_id)
{
	XBT_INFO("JOB CONFIGURATION %d:", configuration_id);
	XBT_INFO("slots: %u map, %u reduce", configs[configuration_id].map_slots, configs[configuration_id].reduce_slots);
	XBT_INFO("chunk replicas: %u", configs[configuration_id].chunk_replicas);
	XBT_INFO("chunk size: %.0f MB", configs[configuration_id].chunk_size / 1024 / 1024);
	XBT_INFO("input chunks: %u", configs[configuration_id].chunk_count);
	XBT_INFO("input size: %g MB", configs[configuration_id].chunk_count * (configs[configuration_id].chunk_size / 1024 / 1024));
	XBT_INFO("maps: %u", configs[configuration_id].number_of_maps);
	XBT_INFO("reduces: %u", configs[configuration_id].number_of_reduces);
	XBT_INFO("workers: %u", configs[configuration_id].number_of_workers);
	XBT_INFO("grid power: %g flops", configs[configuration_id].grid_cpu_power);
	XBT_INFO("average power: %g flops/s", configs[configuration_id].grid_average_speed);
	XBT_INFO("heartbeat interval: %u", configs[configuration_id].heartbeat_interval);
	XBT_INFO(" ");
}

/** @brief  Print job statistics. */
static void print_stats(int configuration_id)
{
	XBT_INFO("JOB STATISTICS %d:", configuration_id);
	XBT_INFO("local maps: %d", statistics[configuration_id].map_local);
	XBT_INFO("non-local maps: %d", statistics[configuration_id].map_remote);
	XBT_INFO("speculative maps (local): %d", statistics[configuration_id].map_spec_l);
	XBT_INFO("speculative maps (remote): %d", statistics[configuration_id].map_spec_r);
	XBT_INFO("total non-local maps: %d", statistics[configuration_id].map_remote + statistics[configuration_id].map_spec_r);
	XBT_INFO("total speculative maps: %d", statistics[configuration_id].map_spec_l + statistics[configuration_id].map_spec_r);
	XBT_INFO("normal reduces: %d", statistics[configuration_id].reduce_normal);
	XBT_INFO("speculative reduces: %d", statistics[configuration_id].reduce_spec);
	XBT_INFO(" ");
}

/**
 * @brief  Checks if a worker is a straggler.
 * @param  worker  The worker to be probed.
 * @return 1 if true, 0 if false.
 */
static int is_straggler(msg_process_t worker, int configuration_id)
{
	size_t task_count;
	size_t wid;
	msg_host_t worker_host;

	wid = get_worker_id(worker);
	worker_host = MSG_process_get_host(worker);

	task_count = (configs[configuration_id].map_slots + configs[configuration_id].reduce_slots)
	        - (w_heartbeats[configuration_id][wid].slots_av[MAP] + w_heartbeats[configuration_id][wid].slots_av[REDUCE]);

	if (MSG_get_host_speed(worker_host) < configs[configuration_id].grid_average_speed && task_count > 0)
		return 1;

	return 0;
}

/**
 * @brief  Returns for how long a task is running.
 * @param  task  The task to be probed.
 * @return The amount of seconds since the beginning of the computation.
 */
static double task_time_elapsed(msg_task_t task)
{
	task_info_t ti;

	ti = (task_info_t) MSG_task_get_data(task);

	return (MSG_task_get_compute_duration(task) - MSG_task_get_remaining_computation(task)) / MSG_get_host_speed(MSG_process_get_host(ti->worker_process));

}

/**
 * @brief  Mark the tasks of a straggler as possible speculative tasks.
 * @param  worker  The straggler worker.
 */
static void set_speculative_tasks(msg_process_t worker, int configuration_id)
{
	size_t tid;
	size_t wid;
	task_info_t ti;

	wid = get_worker_id(worker);

	if (w_heartbeats[configuration_id][wid].slots_av[MAP] < configs[configuration_id].map_slots)
	{
		for (tid = 0; tid < configs[configuration_id].number_of_maps; tid++)
		{
			if (jobs[configuration_id].task_list[MAP][0][tid] != NULL )
			{
				ti = (task_info_t) MSG_task_get_data(jobs[configuration_id].task_list[MAP][0][tid]);
				if (ti->wid == wid && task_time_elapsed(jobs[configuration_id].task_list[MAP][0][tid]) > 60.0)
				{
					jobs[configuration_id].task_status[MAP][tid] = T_STATUS_TIP_SLOW;
				}
			}
		}
	}

	if (w_heartbeats[configuration_id][wid].slots_av[REDUCE] < configs[configuration_id].reduce_slots)
	{
		for (tid = 0; tid < configs[configuration_id].number_of_reduces; tid++)
		{
			if (jobs[configuration_id].task_list[REDUCE][0][tid] != NULL )
			{
				ti = (task_info_t) MSG_task_get_data(jobs[configuration_id].task_list[REDUCE][0][tid]);
				if (ti->wid == wid && task_time_elapsed(jobs[configuration_id].task_list[REDUCE][0][tid]) > 60.0)
				{
					jobs[configuration_id].task_status[REDUCE][tid] = T_STATUS_TIP_SLOW;
				}
			}
		}
	}
}

/**
 * @brief  Choose a map task, and send it to a worker.
 * @param  dest  The destination worker.
 */
static void send_map_to_worker(msg_process_t dest, int configuration_id, FILE* tasks_log)
{
	char* flags;
	int task_type;
	size_t chunk;
	size_t sid = (size_t) NONE;
	size_t tid = (size_t) NONE;
	size_t wid;
	msg_host_t dest_host;

	if (jobs[configuration_id].tasks_pending[MAP] <= 0)
		return;

	enum
	{
		LOCAL, REMOTE, LOCAL_SPEC, REMOTE_SPEC, NO_TASK
	};
	task_type = NO_TASK;

	wid = get_worker_id(dest);
	dest_host = MSG_process_get_host(dest);

	/* Look for a task for the worker. */
	for (chunk = 0; chunk < configs[configuration_id].chunk_count; chunk++)
	{
		if (jobs[configuration_id].task_status[MAP][chunk] == T_STATUS_PENDING)
		{
			if (chunk_owners[configuration_id][chunk][wid])
			{
				task_type = LOCAL;
				tid = chunk;
				break;
			}
			else
			{
				task_type = REMOTE;
				tid = chunk;
			}
		}
		else if (jobs[configuration_id].task_status[MAP][chunk] == T_STATUS_TIP_SLOW && task_type > REMOTE && !jobs[configuration_id].task_has_spec_copy[MAP][chunk])
		{
			if (chunk_owners[configuration_id][chunk][wid])
			{
				task_type = LOCAL_SPEC;
				tid = chunk;
			}
			else if (task_type > LOCAL_SPEC)
			{
				task_type = REMOTE_SPEC;
				tid = chunk;
			}
		}
	}

	switch (task_type)
	{
	case LOCAL:
		flags = "";
		sid = wid;
		statistics[configuration_id].map_local++;
		break;

	case REMOTE:
		flags = "(non-local)";
		sid = find_random_chunk_owner(tid,configuration_id);
		statistics[configuration_id].map_remote++;
		break;

	case LOCAL_SPEC:
		flags = "(speculative)";
		sid = wid;
		jobs[configuration_id].task_has_spec_copy[MAP][tid] = 1;
		statistics[configuration_id].map_spec_l++;
		break;

	case REMOTE_SPEC:
		flags = "(non-local, speculative)";
		sid = find_random_chunk_owner(tid,configuration_id);
		jobs[configuration_id].task_has_spec_copy[MAP][tid] = 1;
		statistics[configuration_id].map_spec_r++;
		break;

	default:
		return;
	}

	XBT_INFO("\tconfig %d map %zu assigned to task tracker\t wid %d\t on %s %s", configuration_id, tid, wid, MSG_host_get_name(dest_host), flags);

	send_task(MAP, tid, sid, dest,configuration_id, tasks_log);
}

/**
 * @brief  Choose a reduce task, and send it to a worker.
 * @param  dest  The destination worker.
 */
static void send_reduce_to_worker(msg_process_t dest, int configuration_id, FILE* tasks_log)
{
	char* flags;
	int task_type;
	size_t t;
	size_t tid = (size_t) NONE;
	msg_host_t dest_host;
	size_t wid;

	dest_host = MSG_process_get_host(dest);
	wid = get_worker_id(dest);

	if (jobs[configuration_id].tasks_pending[REDUCE] <= 0 || ((float) jobs[configuration_id].tasks_pending[MAP] / (float) configs[configuration_id].number_of_maps) > 0.9)
		return;

	enum
	{
		NORMAL, SPECULATIVE, NO_TASK
	};
	task_type = NO_TASK;

	for (t = 0; t < configs[configuration_id].number_of_reduces; t++)
	{
		if (jobs[configuration_id].task_status[REDUCE][t] == T_STATUS_PENDING)
		{
			task_type = NORMAL;
			tid = t;
			break;
		}
		else if (jobs[configuration_id].task_status[REDUCE][t] == T_STATUS_TIP_SLOW && !jobs[configuration_id].task_has_spec_copy[REDUCE][t])
		{
			task_type = SPECULATIVE;
			tid = t;
		}
	}

	switch (task_type)
	{
	case NORMAL:
		flags = "";
		statistics[configuration_id].reduce_normal++;
		break;

	case SPECULATIVE:
		flags = "(speculative)";
		jobs[configuration_id].task_has_spec_copy[REDUCE][tid] = 1;
		statistics[configuration_id].reduce_spec++;
		break;

	default:
		return;
	}

	XBT_INFO("\tconfig %d reduce %zu assigned to task tracker\t wid %d\t on %s %s", configuration_id, tid, wid, MSG_host_get_name(dest_host), flags);

	send_task(REDUCE, tid, (size_t) NONE, dest, configuration_id, tasks_log);
}

/**
 * @brief  Send a task to a worker.
 * @param  phase     The current job phase.
 * @param  tid       The task ID.
 * @param  data_src  The ID of the DataNode that owns the task data.
 * @param  dest      The destination worker.
 */
static void send_task(enum phase_e phase, size_t tid, size_t data_src, msg_process_t dest, int configuration_id,FILE* tasks_log)
{
	char mailbox[MAILBOX_ALIAS_SIZE];
	char sms_task[MAILBOX_ALIAS_SIZE];
	int i;
	double cpu_required = 0.0;
	msg_task_t task = NULL;
	task_info_t task_info;
	size_t wid;

	wid = get_worker_id(dest);

	cpu_required = user.task_cost_f(phase, tid, wid, configuration_id);
	sprintf(sms_task, SMS_TASK, configuration_id);

	task_info = xbt_new (struct task_info_s, 1);
	task = MSG_task_create(sms_task, cpu_required, 0.0, (void*) task_info);

	task_info->phase = phase;
	task_info->id = tid;
	task_info->src = data_src;
	task_info->wid = wid;
	task_info->task = task;
	task_info->shuffle_end = 0.0;

	// for tracing purposes...
	MSG_task_set_category(task, (phase == MAP ? "MAP" : "REDUCE"));

	if (jobs[configuration_id].task_status[phase][tid] != T_STATUS_TIP_SLOW)
		jobs[configuration_id].task_status[phase][tid] = T_STATUS_TIP;

	w_heartbeats[configuration_id][wid].slots_av[phase]--;

	for (i = 0; i < MAX_SPECULATIVE_COPIES; i++)
	{
		if (jobs[configuration_id].task_list[phase][i][tid] == NULL )
		{
			jobs[configuration_id].task_list[phase][i][tid] = task;
			break;
		}
	}

	fprintf(tasks_log, "config%d %d_%zu_%d\t%s\t%zu\t%.3f\tSTART\n",configuration_id, phase, tid, i, (phase == MAP ? "MAP" : "REDUCE"), wid, MSG_get_clock());

#ifdef VERBOSE
	XBT_INFO ("TX: %s > %s", SMS_TASK, MSG_host_get_name (MSG_process_get_host(dest)));
#endif

	sprintf(mailbox, TASKTRACKER_MAILBOX, configuration_id, wid);
	xbt_assert(MSG_task_send(task, mailbox) == MSG_OK, "ERROR SENDING MESSAGE");
}

/**
 * @brief  Kill all copies of a task.
 * @param  ti  The task information of any task instance.
 */
static void finish_all_task_copies(task_info_t ti, int configuration_id, FILE* tasks_log)
{
	int i;
	int phase = ti->phase;
	size_t tid = ti->id;

	for (i = 0; i < MAX_SPECULATIVE_COPIES; i++)
	{
		if (jobs[configuration_id].task_list[phase][i][tid] != NULL )
		{
			MSG_task_cancel(jobs[configuration_id].task_list[phase][i][tid]);
			//FIXME: MSG_task_destroy (job.task_list[phase][i][tid]);
			jobs[configuration_id].task_list[phase][i][tid] = NULL;
			fprintf(tasks_log, "config%d %d_%zu_%d\t-\t-\t%.3f\tEND\t%.3f\n", configuration_id, ti->phase, tid, i, MSG_get_clock(), ti->shuffle_end);
		}
	}
}

