%% ---
%%TopN����ģ��
%%
%%---
-module(topn_manager).
-behaviour(supervisor).

-export([start_link/1,init/1]).

start_link(Args) ->
    supervisor:start_link({local,?MODULE},?MODULE, Args).
    
init(_Args) ->
    {ok, {{one_for_one, 60, 20},
          [
			{topn_siteview, {topn_siteview, start_link, []},transient, brutal_kill, worker, [topn_siteview]}      %% ������
            %%{topn_object_table, {topn_object_table, start_link, []},transient, brutal_kill, worker, [topn_object_table]},     %% ���󱣴���
            %%{topn_storage, {topn_storage, start_link, []},transient, brutal_kill, worker, [topn_storage]}               %% ���ݴ洢��
		  ]}}.