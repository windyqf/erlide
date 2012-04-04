-module(content_store).

-export([start/0, stop/0,ping/0]).

%%-export([start/2, stop/1]).

%-behaviour(application).

-include_lib("stdlib/include/qlc.hrl").

-include("config.hrl").


%%====================================================================
%% External functions
%%====================================================================
%%--------------------------------------------------------------------
%% Func: start/2
%% Returns: {ok, Pid}        |
%%          {ok, Pid, State} |
%%          {error, Reason}   
%%--------------------------------------------------------------------
%start(Type, StartArgs) ->
%        case 'TopSupervisor':start_link(StartArgs) of
%    	{ok, Pid} -> 
%    	    {ok, Pid};
%    	Error ->
%    	    Error
%        end.

start() ->
 %%  	erlide_log:log("*******************Content Store 1*************************"),
	{ok,Cwd} = file:get_cwd(),	
	Path = Cwd ++"/contentstore/db",
	application:set_env(mnesia, dir, list_to_atom(Path)),
    mnesia:create_schema([node()]),
    
    mnesia:start(),
    
    init(),
    
	io:format("*******************Content Store OK*************************~n").
	

%%--------------------------------------------------------------------
%% Func: stop/1
%% Returns: any 
%%--------------------------------------------------------------------
%stop(State) ->
%    mnesia:stop(),
%    ok.
stop() ->
    mnesia:stop(),
    ok.

init() ->

    %mnesia:create_table(content, [{disc_copies, [node()]}, {type, set}, {attributes, record_info(fields, content)}]),
    %mnesia:add_table_index(content, application),

    Tables = mnesia:system_info(tables),
    Fragments = lists:filter(fun(X)-> not lists:member(X, [schema, application, profile]) end, Tables),
    cache:create(fragments),
    lists:foreach(fun(X)->mnesia:create_table(X, [{disc_copies, [node()]}, {index, [application]}, {record_name, document}, {attributes, record_info(fields, document)}]) end, Fragments),
    lists:foreach(fun(X)->cache:set(fragments, X, X) end, Fragments),

    mnesia:create_table(application, [{disc_copies, [node()]},{attributes, record_info(fields, application)}]),

    mnesia:create_table(profile, [{disc_copies, [node()]},{attributes, record_info(fields, profile)}]),
    mnesia:add_table_index(profile, xn_email),
 
    mnesia:wait_for_tables([application], 300000),
    mnesia:wait_for_tables([profile], 300000).

ping()->
	io:format("*******************Content Store pinged OK*************************~n"),
	pong.