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

#include <msg/msg.h>
#include "common.h"
#include "dfs.h"

XBT_LOG_EXTERNAL_DEFAULT_CATEGORY(msg_test);

static void send_data(msg_process_t worker, msg_task_t msg, int configuration_id);

void distribute_data(int configuration_id)
{
	unsigned int chunk;

	/* Allocate memory for the mapping matrix. */
	chunk_owners[configuration_id] = xbt_new (char*, configs[configuration_id].chunk_count);
	for (chunk = 0; chunk < configs[configuration_id].chunk_count; chunk++)
	{
		chunk_owners[configuration_id][chunk] = xbt_new0 (char, configs[configuration_id].number_of_workers);
	}

	/* Call the distribution function. */
	user.dfs_f(chunk_owners[configuration_id], configs[configuration_id].chunk_count, configs[configuration_id].number_of_workers,
	        configs[configuration_id].chunk_replicas, configuration_id);
}

void default_dfs_f(char** dfs_matrix, size_t chunks, size_t workers, unsigned int replicas, int configuration_id)
{
	unsigned int r;
	unsigned int chunk;
	unsigned int owner;

	if (configs[configuration_id].chunk_replicas >= configs[configuration_id].number_of_workers)
	{
		/* All workers own every chunk. */
		for (chunk = 0; chunk < configs[configuration_id].chunk_count; chunk++)
		{
			for (owner = 0; owner < configs[configuration_id].number_of_workers; owner++)
			{
				chunk_owners[configuration_id][chunk][owner] = 1;
			}
		}
	}
	else
	{
		/* Ok, it's a typical distribution. */
		for (chunk = 0; chunk < configs[configuration_id].chunk_count; chunk++)
		{
			for (r = 0; r < configs[configuration_id].chunk_replicas; r++)
			{
				owner = ((chunk % configs[configuration_id].number_of_workers)
				        + ((configs[configuration_id].number_of_workers / configs[configuration_id].chunk_replicas) * r))
				        % configs[configuration_id].number_of_workers;

				chunk_owners[configuration_id][chunk][owner] = 1;
			}
		}
	}
}

unsigned int find_random_chunk_owner(size_t cid, int configuration_id)
{
	size_t replica;
	size_t owner = (size_t) NONE;
	size_t wid;

	replica = (size_t) rand() % configs[configuration_id].chunk_replicas;

	for (wid = 0; wid < configs[configuration_id].number_of_workers; wid++)
	{
		if (chunk_owners[configuration_id][cid][wid])
		{
			owner = wid;

			if (replica == 0)
				break;
			else
				replica--;
		}
	}

	xbt_assert(owner != (size_t)NONE, "Aborted: chunk %zu is missing.", cid);

	return owner;
}

int data_node(int argc, char* argv[])
{
	char mailbox[MAILBOX_ALIAS_SIZE];
	char message_finish[MAILBOX_ALIAS_SIZE];
	msg_task_t msg = NULL;
	int parent_id;
	msg_process_t parent_process;
	int config_id;

	xbt_assert(argc >= 1, "data_node function requires at least 1 argument - its config ID");
	sscanf(argv[0], "%d", &config_id);

	parent_id = MSG_process_self_PPID();
	parent_process = MSG_process_from_PID(parent_id);

	sprintf(mailbox, DATANODE_MAILBOX, config_id, get_worker_id(parent_process));
	sprintf(message_finish, SMS_FINISH, config_id);

	while (!jobs[config_id].finished)
	{
		msg = NULL;
		receive(&msg, mailbox);
		if (message_is(msg, message_finish))
		{
			MSG_task_destroy(msg);
			break;
		}
		else
		{
			send_data(parent_process, msg, config_id);
		}
	}

	return 0;
}

static void send_data(msg_process_t worker, msg_task_t msg, int configuration_id)
{
	char mailbox[MAILBOX_ALIAS_SIZE];
	char message_get_chunk[MAILBOX_ALIAS_SIZE];
	char message_get_inter_pairs[MAILBOX_ALIAS_SIZE];
	double data_size;
	size_t my_id;
	task_info_t ti;
	msg_process_t original_worker;

	my_id = get_worker_id(worker);

	ti = (task_info_t) MSG_task_get_data(msg);
	original_worker = ti->worker_process;

	sprintf(mailbox, TASK_MAILBOX, configuration_id, get_worker_id(original_worker), MSG_process_get_PID(MSG_task_get_sender(msg)));
	sprintf(message_get_chunk, SMS_GET_CHUNK, configuration_id);
	sprintf(message_get_inter_pairs, SMS_GET_INTER_PAIRS, configuration_id);

	if (message_is(msg, message_get_chunk))
	{
		//simulate disk access to retrieve the data
		{
			msg_file_t file = NULL;
			void *ptr = NULL;
			double read;

			file = MSG_file_open("/home", "./disk/disk.disk", "rw");

			read = MSG_file_read(ptr, (size_t) configs[configuration_id].chunk_size, sizeof(char*), file);

#ifdef VERBOSE
			XBT_INFO("\tDFS read    %8.1f on %s", read, file->name);
			s_msg_stat_t stat;
			MSG_file_stat(file, &stat);
			XBT_INFO("\tFile stat %s Size %.1f", file->name, stat.size);
			MSG_file_free_stat(&stat);
#endif
			MSG_file_close(file);

		}
		MSG_task_dsend(MSG_task_create("DATA-C", 0.0, configs[configuration_id].chunk_size, NULL ), mailbox, NULL );
	}
	else if (message_is(msg, message_get_inter_pairs))
	{
		data_size = (double) (jobs[configuration_id].map_output[my_id][ti->id] - ti->map_output_copied[my_id]);
		//simulate disk access to retrieve the data
		{
			msg_file_t file = NULL;
			void *ptr = NULL;
			double read;

			file = MSG_file_open("/home", "./disk/disk.disk", "rw");

			read = MSG_file_read(ptr, (size_t) data_size, sizeof(char*), file);

#ifdef VERBOSE
			XBT_INFO("\tDFS read    %8.1f on %s", read, file->name);
			s_msg_stat_t stat;
			MSG_file_stat(file, &stat);
			XBT_INFO("\tFile stat %s Size %.1f", file->name, stat.size);
			MSG_file_free_stat(&stat);
#endif
			MSG_file_close(file);

		}
#ifdef VERBOSE
		XBT_INFO("\tDATA-IP    %f mailbox %s my id %d from %s to %s", data_size, mailbox, my_id, MSG_host_get_name(MSG_process_get_host(worker)),
				MSG_host_get_name(MSG_process_get_host(MSG_task_get_sender(msg))));
#endif
		if (strcmp(MSG_host_get_name(MSG_process_get_host(worker)), MSG_host_get_name(MSG_process_get_host(MSG_task_get_sender(msg)))))
		{
			MSG_task_dsend(MSG_task_create("DATA-IP", 0.0, data_size, NULL ), mailbox, NULL );
		}
		else
		{
			MSG_task_dsend(MSG_task_create("DATA-IP", data_size, -1, NULL ), mailbox, NULL );
		}
	}

	MSG_task_destroy(msg);
}

