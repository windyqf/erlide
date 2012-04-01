-module(queue_client).
-behaviour(gen_server).

-include("../include/amqp_client.hrl").
-compile(export_all).

-export([start_link/0,stop/0]).

-export([asyncCall/1]).

%-record(state, {}).
-record(state, {conn}).

%% gen_server callbacks
-export([init/1, handle_call/3, handle_cast/2, handle_info/2, code_change/3, terminate/2]).

start_link() ->
    gen_server:start_link({local, ?MODULE}, ?MODULE, [], []).

stop() ->
    gen_server:cast(?MODULE,stop).


init([]) ->
	process_flag(trap_exit, true),
	P = init(),
	{ok, #state{conn = P}}.

init()->
      %% Start a network connection
      {ok, Connection} = amqp_connection:start(network, #amqp_params{}),
	  io:format("Connection:~p~n",[Connection]),
      %% Open a channel on the connection
      {ok, Channel} = amqp_connection:open_channel(Connection),
%% 	  io:format("Channel:~p~n",[Channel]),

%%	1 input
	  Exchange_Input = <<"siteveiw_input_exchange">>,
	  Queue_Input = <<"siteveiw_input_queue">>,
      BindKey_Input = <<"siteveiw_input.#">>,
	   
       #'queue.declare_ok'{queue = Queue_Input}
                = amqp_channel:call(Channel, #'queue.declare'{queue = Queue_Input}),
	  
       #'exchange.declare_ok'{} 
				= amqp_channel:call(Channel, #'exchange.declare'{exchange = Exchange_Input, 
																 type = <<"topic">>}),

       #'queue.bind_ok'{} 
				= amqp_channel:call(Channel, #'queue.bind'{queue = Queue_Input, 
														   exchange = Exchange_Input,  
														   routing_key = BindKey_Input}),
      RoutingKey_Input_Default = <<"siteveiw_input.client">>,
%%     Payload = <<"This is a really interesting message!">>,
%%     [send_message(Channel, Exchange_Input, RoutingKey_Input, Payload) || Tag <- lists:seq(1, 100)],
%% 	   setup_consumer(Channel, Queue_Input).
	  
%%	2 output1
	  Exchange_Output1 = <<"siteveiw_output_exchange1">>,
	  Queue_Output1 = <<"siteveiw_output_queue1">>,
      BindKey_Output1 = <<"siteveiw_output1.#">>,
	   
       #'queue.declare_ok'{queue = Queue_Output1}
                = amqp_channel:call(Channel, #'queue.declare'{queue = Queue_Output1}),
	  
       #'exchange.declare_ok'{} 
				= amqp_channel:call(Channel, #'exchange.declare'{exchange = Exchange_Output1, 
																 type = <<"topic">>}),

       #'queue.bind_ok'{} 
				= amqp_channel:call(Channel, #'queue.bind'{queue = Queue_Output1, 
														   exchange = Exchange_Output1,  
														   routing_key = BindKey_Output1}),
      RoutingKey_Output1_Default = <<"siteveiw_output1.client1">>,
	  
%%	3 output2 + consumer	  
	  Exchange_Output2 = <<"siteveiw_output_exchange2">>,
	  Queue_Output2 = <<"siteveiw_output_queue2">>,
      BindKey_Output2 = <<"siteveiw_output2.#">>,
	   
       #'queue.declare_ok'{queue = Queue_Output2}
                = amqp_channel:call(Channel, #'queue.declare'{queue = Queue_Output2}),
	  
       #'exchange.declare_ok'{} 
				= amqp_channel:call(Channel, #'exchange.declare'{exchange = Exchange_Output2, 
																 type = <<"topic">>}),

       #'queue.bind_ok'{} 
				= amqp_channel:call(Channel, #'queue.bind'{queue = Queue_Output2, 
														   exchange = Exchange_Output2,  
														   routing_key = BindKey_Output2}),
      RoutingKey_Output2_Default = <<"siteveiw_output2.client2">>,	  
	  
%% 	4 cache  
	cache:create(queue_client_buffer),
%% 	cache:create(queue_client_callback_buffer),
%% 	setup_consumer(Channel, Queue_Output2),
    ConsumerId = spawn_opt(?MODULE, setup_consumer, [Channel, Queue_Output2], [{priority, high}]),
	io:format("init ConProcId:  ~p ~n", [ConsumerId]),

%% 	5 parameter	  
	 {{connection, Connection}, {chanel, Channel}, 
	   {input_routing_key, RoutingKey_Input_Default}, {input_exchange, Exchange_Input},{input_queue, Queue_Input},
	   {output1_routing_key, RoutingKey_Output1_Default}, {output1_exchange, Exchange_Output1},{output1_queue, Queue_Output1},
	   {output2_routing_key, RoutingKey_Output2_Default}, {output2_exchange, Exchange_Output2},{output2_queue, Queue_Output2}}.

%% setup_consumer(Param) ->
%%  {{connection, Connection}, {chanel, Channel}, 
%% 	{input_routing_key, RoutingKey_Input_Default}, {input_exchange, Exchange_Input},{input_queue, Queue_Input},
%% 	{output1_routing_key, RoutingKey_Output1_Default}, {output1_exchange, Exchange_Output1},{output1_queue, Queue_Output1},
%% 	{output2_routing_key, RoutingKey_Output2_Default}, {output2_exchange, Exchange_Output2},{output2_queue, Queue_Output2}} = Param,	
%% 	setup_consumer(Channel, Queue_Output2).

asyncCall(Msg)->
	gen_server:call(?MODULE, {asyncCall,Msg}, infinity).

handle_call({asyncCall, Msg}, _, State) ->
	{state,Param} = State,
  {{connection, Connection}, {chanel, Channel}, 
	{input_routing_key, RoutingKey_Input_Default}, {input_exchange, Exchange_Input},{input_queue, Queue_Input},
	{output1_routing_key, RoutingKey_Output1_Default}, {output1_exchange, Exchange_Output1},{output1_queue, Queue_Output1},
	{output2_routing_key, RoutingKey_Output2_Default}, {output2_exchange, Exchange_Output2},{output2_queue, Queue_Output2}} = Param,
	Guid = erlang:binary_to_list(uuid()),
  	BasicPublish = #'basic.publish'{exchange = Exchange_Input, routing_key = RoutingKey_Input_Default},
%%     Props = #'P_basic'{correlation_id = <<Guid:64>>, content_type = <<"application/octet-stream">>, reply_to = Queue_Output2},
	Props = #'P_basic'{message_id = <<1:64>>, content_type = <<"application/octet-stream">>, reply_to = Queue_Output2},
  	amqp_channel:cast(Channel, BasicPublish, _MsgPayload = #amqp_msg{props = Props, payload = erlang:term_to_binary({Guid, 1, Msg})}),
	{reply,Guid,State};
handle_call({asyncCall, Msg, CallProcId}, _, State) ->
  {state,Param} = State,
  {{connection, Connection}, {chanel, Channel}, 
	{input_routing_key, RoutingKey_Input_Default}, {input_exchange, Exchange_Input},{input_queue, Queue_Input},
	{output1_routing_key, RoutingKey_Output1_Default}, {output1_exchange, Exchange_Output1},{output1_queue, Queue_Output1},
	{output2_routing_key, RoutingKey_Output2_Default}, {output2_exchange, Exchange_Output2},{output2_queue, Queue_Output2}} = Param,
	Guid = erlang:binary_to_list(uuid()),
%%   	cache:set(queue_client_callback_buffer, {Guid, callback}, CallBack),
  	Props = #'P_basic'{message_id = <<2:64>>, content_type = <<"application/octet-stream">>, reply_to = Queue_Output1},
  	BasicPublish = #'basic.publish'{exchange = Exchange_Input, routing_key = RoutingKey_Input_Default},
  	amqp_channel:cast(Channel, BasicPublish, _MsgPayload = #amqp_msg{props = Props, payload = erlang:term_to_binary({Guid, 2, Msg})}),  
	{reply,Guid,State};
handle_call({asyncCallResult, Guid}, _, State) ->
  {state,Param} = State,
  {{connection, Connection}, {chanel, Channel}, 
	{input_routing_key, RoutingKey_Input_Default}, {input_exchange, Exchange_Input},{input_queue, Queue_Input},
	{output1_routing_key, RoutingKey_Output1_Default}, {output1_exchange, Exchange_Output1},{output1_queue, Queue_Output1},
	{output2_routing_key, RoutingKey_Output2_Default}, {output2_exchange, Exchange_Output2},{output2_queue, Queue_Output2}} = Param,	
	{reply,cache:get(queue_client_buffer, Guid),State};
handle_call({asyncCallResult_Server}, _, State) ->
  {state,Param} = State,
  {{connection, Connection}, {chanel, Channel}, 
	{input_routing_key, RoutingKey_Input_Default}, {input_exchange, Exchange_Input},{input_queue, Queue_Input},
	{output1_routing_key, RoutingKey_Output1_Default}, {output1_exchange, Exchange_Output1},{output1_queue, Queue_Output1},
	{output2_routing_key, RoutingKey_Output2_Default}, {output2_exchange, Exchange_Output2},{output2_queue, Queue_Output2}} = Param,
  %% Get the message back from the queue
     Get = #'basic.get'{queue = Queue_Output1},
	 {#'basic.get_ok'{delivery_tag = Tag}, Content} =  amqp_channel:call(Channel, Get),
	  %% Do something with the message payload
      %% (some work here)	  
%%  	 io:format("Content:~p~n",[Content]),	   
     %% Ack the message
     amqp_channel:cast(Channel, #'basic.ack'{delivery_tag = Tag}),	
	{reply,Content,State};
handle_call(Req, _, State) ->
    {reply, {error, {unknown_request, Req}}, State}.

send_message(Param, RoutingKey, Payload) ->
%%     log(send_message,"basic.publish setup"),
	   {{connection, Connection}, {chanel, Channel}, 
		{input_routing_key, RoutingKey_Input_Default}, {input_exchange, Exchange_Input},{input_queue, Queue_Input},
		{output1_routing_key, RoutingKey_Output1_Default}, {output1_exchange, Exchange_Output1},{output1_queue, Queue_Output1},
		{output2_routing_key, RoutingKey_Output2_Default}, {output2_exchange, Exchange_Output2},{output2_queue, Queue_Output2}} = Param,	
	case RoutingKey of
		default->
		    BasicPublish = #'basic.publish'{exchange = Exchange_Input, routing_key = RoutingKey_Input_Default};
		_->
			BasicPublish = #'basic.publish'{exchange = Exchange_Input, routing_key = RoutingKey}
	end,
%%     log(send_message,"amqp_channel:cast"),
    ok = amqp_channel:cast(Channel, BasicPublish, _MsgPayload = #amqp_msg{payload = Payload}).

send_message(Channel, X, RoutingKey, Payload) ->
%%     log(send_message,"basic.publish setup"),
    BasicPublish = #'basic.publish'{exchange = X, routing_key = RoutingKey},

%%     log(send_message,"amqp_channel:cast"),
    ok = amqp_channel:cast(Channel, BasicPublish, _MsgPayload = #amqp_msg{payload = Payload}).

close(Param) ->
	   {{connection, Connection}, {chanel, Channel}, 
		{input_routing_key, RoutingKey_Input_Default}, {input_exchange, Exchange_Input},{input_queue, Queue_Input},
		{output1_routing_key, RoutingKey_Output1_Default}, {output1_exchange, Exchange_Output1},{output1_queue, Queue_Output1},
		{output2_routing_key, RoutingKey_Output2_Default}, {output2_exchange, Exchange_Output2},{output2_queue, Queue_Output2}} = Param, 
	    
	    log(channel_close,"start"),
    	ok = amqp_channel:close(Channel),

	    log(connection_close,"start"),
    	ok = amqp_connection:close(Connection),
    	log(connection_close,"Demo Completed!"),
    	ok.

setup_consumer(Channel, Q) ->
    %% Register a consumer to listen to a queue
    log(setup_consumer,"basic.consume"),
    BasicConsume = #'basic.consume'{queue = Q,
                                    consumer_tag = <<"">>,
                                    no_ack = true},
    #'basic.consume_ok'{consumer_tag = ConsumerTag}
                     = amqp_channel:subscribe(Channel, BasicConsume, self()),

    %% If the registration was sucessful, then consumer will be notified
    log(setup_consumer,"basic.consume_ok start receive"),
    receive
        #'basic.consume_ok'{consumer_tag = ConsumerTag} -> ok
    end,
    log(setup_consumer,"basic.consume_ok finished"),

    %% When a message is routed to the queue, it will then be delivered to this consumer
    log(read_messages,"start"),
	read_messages().
%%     Msg = read_messages(0),
%%     io:format("Msg: ~p~n", [Msg]),
%%     log(read_messages,"finish"),
%% 
%%     %% After the consumer is finished interacting with the queue, it can deregister itself
%%     log(basic_cancel,"start"),
%%     BasicCancel = #'basic.cancel'{consumer_tag = ConsumerTag},
%%     #'basic.cancel_ok'{consumer_tag = ConsumerTag} = amqp_channel:call(Channel,BasicCancel).

read_messages()->
    receive
        {#'basic.deliver'{consumer_tag=_ConsumerTag, delivery_tag=_DeliveryTag, redelivered=_Redelivered, exchange=_Exchange, routing_key=RoutingKey}, Content} ->
%%	          log(read_messages,"basic.deliver"),
%%             io:format("RoutingKey received: ~p~n", [RoutingKey]),
            #amqp_msg{payload = Payload} = Content,
%%             io:format("queue_client Payload received: ~p~n", [erlang:binary_to_term(Payload)]),
%% 			{Guid, Type, MsgContent} = erlang:binary_to_term(Payload),			
			{Guid, MsgContent} = erlang:binary_to_term(Payload),
			cache:set(queue_client_buffer, Guid, MsgContent),
            read_messages();
        Any ->
            io:format("received unexpected Any: ~p~n", [Any]),
            read_messages()
%%     after 1000 ->
%%         case Timeouts of
%%             0 ->
%%                 Timeouts2 = Timeouts + 1,
%%                 read_messages(Timeouts2);
%%             10000 ->
%%                 io:format("~n"),
%%                 io:format("Message timeout exceeded ~n");
%% %% 				read_messages(0);
%%             _ ->
%%                 Timeouts2 = Timeouts + 1,
%%                 io:format("."),
%%                 read_messages(Timeouts2)
%%         end
    end.

log(Key,Value) ->
    io:format("queue_client:~p: ~p~n",[Key,Value]).

uuid() ->
    {A, B, C} = now(),
    <<A:32, B:32, C:32>>.

handle_cast(stop, State) ->	
%% 	pgsql:close(State#state.conn),
	close(State),
	{stop,normal,State};
handle_cast(_, State) ->	
    {noreply, State}.

handle_info(_, State) ->
    {noreply, State}.

terminate(_Reason, _State) ->
    ok.

code_change({down, _Vsn}, State, _Extra) ->
    {ok, State};

% upgrade
code_change(_Vsn, State, _Extra) ->
    {ok, State}.