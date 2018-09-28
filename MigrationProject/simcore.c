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
#include <xbt/sysdep.h>
#include <xbt/log.h>
#include <xbt/asserts.h>
#include "common.h"
#include "dfs.h"
#include "mrsg.h"

XBT_LOG_NEW_DEFAULT_CATEGORY(msg_test, "MRSG");

#define MAX_LINE_SIZE 256

int scheduler(int argc, char *argv[]);

static msg_error_t run_simulation();

static void read_mr_config_file();

int MRSG_main(const char* plat, const char* conf)
{
	int argc = 8;
	char* argv[] =
	{ "mrsg", "--cfg=tracing:1", "--cfg=tracing/buffer:1", "--cfg=tracing/filename:tracefile.trace", "--cfg=tracing/categorized:1",
	        "--cfg=tracing/uncategorized:1", "--cfg=viva/categorized:cat.plist", "--cfg=viva/uncategorized:uncat.plist" };

	msg_error_t res = MSG_OK;

	// Check if the user configuration is sound.
	xbt_assert(user.task_cost_f != NULL, "Task cost function not specified.");
	xbt_assert(user.map_output_f != NULL, "Map output function not specified.");
	xbt_assert(user.dfs_f != NULL, "DFS function not specified.");

	MSG_init(&argc, argv);

	read_mr_config_file(conf);

	MSG_create_environment(plat);

	res = run_simulation(plat);

	if (res == MSG_OK)
		return 0;
	else
		return 1;
}

int ordering_function(const void* a, const void* b)
{
	msg_host_t* system_host1 = (msg_host_t*) a;
	msg_host_t* system_host2 = (msg_host_t*) b;
	const char* name1 = MSG_host_get_name(*system_host1);
	const char* name2 = MSG_host_get_name(*system_host2);

	return strcmp(name1, name2);
}

static msg_error_t run_simulation()
{
	msg_error_t res = MSG_OK;
	xbt_dynar_t hosts_dynar;

	// for tracing purposes..
	TRACE_category_with_color("MAP", "1 0 0");
	TRACE_category_with_color("REDUCE", "0 0 1");

	/* Retrieve the first hosts of the platform file */
	hosts_dynar = MSG_hosts_as_dynar();
	xbt_dynar_sort(hosts_dynar, ordering_function);

	master_host = xbt_dynar_get_as(hosts_dynar,0,msg_host_t);
	xbt_assert(master_host, "UNABLE TO IDENTIFY THE MASTER NODE");

	srand(12345);
	//we start scheduler and a Map-Reduce job tracker (master) on host 0
	MSG_process_create("scheduler", scheduler, NULL, master_host);
	res = MSG_main();

	XBT_INFO("The END!");

	xbt_dynar_free(&hosts_dynar);

	xbt_free(configs);
	xbt_free(w_heartbeats);
	xbt_free(jobs);
	xbt_free(statistics);
	xbt_free(chunk_owners);

	return res;
}

/**
 * @brief  Read the MapReduce configuration file.
 * @param  file_name  The path/name of the configuration file.
 */
static void read_mr_config_file(const char* config_collection_file_name)
{

	FILE* config_collection_file;
	char* config_file_name;
	char line_counter[1024];
	unsigned int linesnum = 0;
	char property[256];
	FILE* config_file;
	int i;
	size_t len;
	ssize_t read;

	master_host = NULL;
	w_heartbeats = NULL;
	configs = NULL;
	jobs = NULL;
	statistics = NULL;
	configs_count = 0;
	hosts_required = 0;

	config_collection_file = fopen(config_collection_file_name, "r");
	while (fgets(line_counter, sizeof(line_counter), config_collection_file) != NULL )
	{
		linesnum++;
	}

	if (linesnum > 0)
	{
		configs_count = (int) linesnum;
		config_collection_file = fopen(config_collection_file_name, "r");
		configs = xbt_new(struct config_s,linesnum);
		w_heartbeats = xbt_new(heartbeat_t,linesnum);
		jobs = xbt_new(struct job_s,linesnum);
		statistics = xbt_new(struct stats_s,linesnum);
		chunk_owners = xbt_new(char**,linesnum);

		for (i = 0; i < configs_count; i++)
		{
			len = 0;
			config_file_name = xbt_new(char, 1024);
			read = getline(&config_file_name, &len, config_collection_file);
			size_t ln = strlen(config_file_name) - 1;
			if (config_file_name[ln] == '\n')
				config_file_name[ln] = '\0';

			if (read != -1)
			{
				//fgets(config_file_name, sizeof(config_file_name), config_collection_file);

				config_file = fopen(config_file_name, "r");
				xbt_assert(config_file != NULL, "Error reading cofiguration file: %s", config_file_name);

				/* Set the default configuration. */
				configs[i].chunk_size = 67108864;
				configs[i].chunk_count = 0;
				configs[i].chunk_replicas = 3;
				configs[i].map_slots = 2;
				configs[i].number_of_reduces = 1;
				configs[i].reduce_slots = 2;
				configs[i].worker_hosts_number = 40;
				configs[i].vm_per_host = 2;

				//
				configs[i].cpu_flops_map = 100000000000;
				configs[i].ram_operations_map = 1000000;
				configs[i].disk_operations_map = 10000;

				configs[i].map_output_bytes = (unsigned long long) 1024 * 1024 * 1024;

				configs[i].cpu_flops_reduce = 500000000000;
				configs[i].ram_operations_reduce = 1000000;
				configs[i].disk_operations_reduce = 10000;
				/* Read the user configuration file. */

				while (fscanf(config_file, "%256s", property) != EOF)
				{
					if (strcmp(property, "chunk_size") == 0)
					{
						fscanf(config_file, "%lg", &configs[i].chunk_size);
						configs[i].chunk_size *= 1024 * 1024; /* MB -> bytes */
					}
					else if (strcmp(property, "input_chunks") == 0)
					{
						fscanf(config_file, "%u", &configs[i].chunk_count);
					}
					else if (strcmp(property, "dfs_replicas") == 0)
					{
						fscanf(config_file, "%u", &configs[i].chunk_replicas);
					}
					else if (strcmp(property, "map_slots") == 0)
					{
						fscanf(config_file, "%u", &configs[i].map_slots);
					}
					else if (strcmp(property, "reduces") == 0)
					{
						fscanf(config_file, "%u", &configs[i].number_of_reduces);
					}
					else if (strcmp(property, "reduce_slots") == 0)
					{
						fscanf(config_file, "%u", &configs[i].reduce_slots);
					}
					else if (strcmp(property, "worker_hosts_number") == 0)
					{
						fscanf(config_file, "%ld", &configs[i].worker_hosts_number);
					}
					else if (strcmp(property, "vm_per_host") == 0)
					{
						fscanf(config_file, "%ld", &configs[i].vm_per_host);
					}
					else if (strcmp(property, "cpu_flops_map") == 0)
					{
						fscanf(config_file, "%lg", &configs[i].cpu_flops_map);
					}
					else if (strcmp(property, "ram_operations_map") == 0)
					{
						fscanf(config_file, "%lld", &configs[i].ram_operations_map);
					}
					else if (strcmp(property, "disk_operations_map") == 0)
					{
						fscanf(config_file, "%lld", &configs[i].disk_operations_map);
					}
					else if (strcmp(property, "map_output_bytes") == 0)
					{
						fscanf(config_file, "%lld", &configs[i].map_output_bytes);
					}
					else if (strcmp(property, "cpu_flops_reduce") == 0)
					{
						fscanf(config_file, "%lg", &configs[i].cpu_flops_reduce);
					}
					else if (strcmp(property, "ram_operations_reduce") == 0)
					{
						fscanf(config_file, "%lld", &configs[i].ram_operations_reduce);
					}
					else if (strcmp(property, "disk_operations_reduce") == 0)
					{
						fscanf(config_file, "%lld", &configs[i].disk_operations_reduce);
					}
					else
					{
						printf("Error: Property %s is not valid. (in %s)", property, config_file_name);
						exit(1);
					}
				}

				fclose(config_file);
			}
			if (config_file_name)
				xbt_free(config_file_name);

			/* Assert the configuration values. */

			xbt_assert(configs[i].chunk_size > 0, "Chunk size must be greater than zero");
			xbt_assert(configs[i].chunk_count > 0, "The amount of input chunks must be greater than zero");
			xbt_assert(configs[i].chunk_replicas > 0, "The amount of chunk replicas must be greater than zero");
			xbt_assert(configs[i].map_slots > 0, "Map slots must be greater than zero");
			xbt_assert(configs[i].reduce_slots > 0, "Reduce slots must be greater than zero");
			xbt_assert(configs[i].worker_hosts_number > 0, "Number of worker hosts must be greater than zero");
			xbt_assert(configs[i].vm_per_host > 0, "Number of virtual machines per host must be greater than zero");


			xbt_assert(configs[i].cpu_flops_map > 0, "Number of CPU FLOPS for map stage must be greater than zero");
			xbt_assert(configs[i].map_output_bytes > 0, "Number of bytes that map task sends to reduce must be greater than zero");

			xbt_assert(configs[i].cpu_flops_reduce > 0, "Number of CPU FLOPS for reduce stage must be greater than zero");
		}
		fclose(config_collection_file);
	}
}
