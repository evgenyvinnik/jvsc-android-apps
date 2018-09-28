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

#ifndef MRSG_COMMON_H
#define MRSG_COMMON_H

#include <msg/msg.h>
#include <xbt/sysdep.h>
#include <xbt/log.h>
#include <xbt/asserts.h>
#include "mrsg.h"

//#define VERBOSE 1

/* Short message names. */
#define SMS_GET_CHUNK "%d:SMS-GC"
#define SMS_GET_INTER_PAIRS "%d:SMS-GIP"
#define SMS_HEARTBEAT "SMS-HB%d"
#define SMS_TASK "SMS-T%d"
#define SMS_TASK_DONE "SMS-TD%d"
#define SMS_FINISH "%d:SMS-F"

#define NONE (-1)
#define MAX_SPECULATIVE_COPIES 3

/* Mailbox related. */
#define MAILBOX_ALIAS_SIZE 256
#define MASTER_MAILBOX "MASTER%d"
#define DATANODE_MAILBOX "%d:%zu:DN"
#define TASKTRACKER_MAILBOX "%d:%zu:TT"
#define TASK_MAILBOX "%d:%zu:%d"

/** @brief  Communication ports. */
enum port_e
{
	PORT_MASTER, PORT_DATA_REQ, PORT_SLOTS_START
};

/** @brief  Possible task status. */
enum task_status_e
{
	/* The initial status must be the first enum. */
	T_STATUS_PENDING, T_STATUS_TIP, T_STATUS_TIP_SLOW, T_STATUS_DONE
};

struct config_s
{
	double chunk_size;
	double grid_average_speed;
	double grid_cpu_power;
	unsigned int chunk_count;
	unsigned int chunk_replicas;
	unsigned int heartbeat_interval;
	unsigned int map_slots;
	unsigned int number_of_maps;
	unsigned int number_of_reduces;
	unsigned int number_of_workers;
	unsigned int reduce_slots;
	int initialized;
	unsigned long int worker_hosts_number;
	unsigned long int vm_per_host;

	double cpu_flops_map;
	unsigned long long int ram_operations_map;
	unsigned long long int disk_operations_map;
	unsigned long long int map_output_bytes;
	double cpu_flops_reduce;
	unsigned long long int ram_operations_reduce;
	unsigned long long int disk_operations_reduce;
};

typedef struct config_s* config_t;

struct job_s
{
	int finished;
	unsigned int tasks_pending[2];
	int* task_has_spec_copy[2];
	int* task_status[2];
	msg_task_t** task_list[2];
	unsigned long long** map_output;
};

typedef struct job_s* job_t;

/** @brief  Information sent as the task data. */
struct task_info_s
{
	enum phase_e phase;
	size_t id;
	size_t src;
	size_t wid;
	int pid;
	msg_task_t task;
	unsigned long long* map_output_copied;
	double shuffle_end;
	msg_process_t worker_process;
};

typedef struct task_info_s* task_info_t;

/** @brief  Information sent by the workers with every heartbeat. */
struct heartbeat_s
{
	unsigned int slots_av[2];
};

typedef struct heartbeat_s* heartbeat_t;

struct stats_s
{
	int map_local;
	int map_remote;
	int map_spec_l;
	int map_spec_r;
	int reduce_normal;
	int reduce_spec;
	int* maps_processed;
	int* reduces_processed;
};
typedef struct stats_s* stats_t;

struct user_s
{
	double (*task_cost_f)(enum phase_e phase, size_t tid, size_t wid, int configuration_id);
	void (*dfs_f)(char** dfs_matrix, size_t chunks, size_t workers,
	        unsigned int replicas, int configuration_id);
	unsigned long long (*map_output_f)(size_t mid, size_t rid, int configuration_id);
} user;

msg_host_t master_host;
heartbeat_t* w_heartbeats;
config_t configs;
job_t jobs;
stats_t statistics;
int configs_count;
int hosts_required;

/**
 * @brief  Get the ID of a worker.
 * @param  worker  The worker node.
 * @return The worker's ID number.
 */
size_t get_worker_id(msg_process_t worker);

/** 
 * @brief  Send a message/task.
 * @param  str      The message.
 * @param  cpu      The amount of cpu required by the task.
 * @param  net      The message size in bytes.
 * @param  data     Any data to attatch to the message.
 * @param  mailbox  The destination mailbox alias.
 */
void send(const char* str, double cpu, double net, void* data,
        const char* mailbox);

/** 
 * @brief  Send a short message, of size zero.
 * @param  str      The message.
 * @param  mailbox  The destination mailbox alias.
 */
void send_sms(const char* str, const char* mailbox);

/** 
 * @brief  Receive a message/task from a mailbox.
 * @param  msg      Where to store the received message.
 * @param  mailbox  The mailbox alias.
 * @return The status of the transfer.
 */
msg_error_t receive(msg_task_t* msg, const char* mailbox);

/** 
 * @brief  Compare the message from a task with a string.
 * @param  msg  The message/task.
 * @param  str  The string to compare with.
 * @return A positive value if matches, zero if doesn't.
 */
int message_is(msg_task_t msg, const char* str);

/**
 * @brief  Return the maximum of two values.
 */
unsigned maxval(unsigned int a, unsigned int b);

#endif /* !MRSG_COMMON_H */
