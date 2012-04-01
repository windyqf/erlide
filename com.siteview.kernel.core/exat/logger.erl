%
% logger.erl
%
% ----------------------------------------------------------------------
%
%  eXAT, an erlang eXperimental Agent Tool
%  Copyright (C) 2005-07 Corrado Santoro (csanto@diit.unict.it)
%
%  This program is free software: you can redistribute it and/or modify
%  it under the terms of the GNU General Public License as published by
%  the Free Software Foundation, either version 3 of the License, or
%  (at your option) any later version.
%
%  This program is distributed in the hope that it will be useful,
%  but WITHOUT ANY WARRANTY; without even the implied warranty of
%  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
%  GNU General Public License for more details.
%
%  You should have received a copy of the GNU General Public License
%  along with this program.  If not, see <http://www.gnu.org/licenses/>
%
%
-module (logger).
-behaviour (gen_event).
%%====================================================================
%% Include files
%%====================================================================

%%====================================================================
%% External exports
%%====================================================================

-export ([start/1,
          log/2,
          stop/1]).

%%====================================================================
%% Internal exports
%%====================================================================

-export ([init/1,
          handle_event/2,
          terminate/2,
          code_change/3]).

%%====================================================================
%% External functions
%%====================================================================
%%====================================================================
%% Function: start/1
%% Description: Starts a new logger
%%====================================================================

start (LoggerName) ->
  gen_event:start ({local, LoggerName}),
  gen_event:add_handler (LoggerName, logger, LoggerName).

%%====================================================================
%% Function: stop/1
%% Description: Stops a logger
%%====================================================================

stop (LoggerName) ->
  gen_event:stop (LoggerName).

%%====================================================================
%% Function: log/2
%% Description: Logs a message
%%====================================================================
log (LoggerName, Message) ->
  gen_event:notify (LoggerName, Message).


%%====================================================================
%% Internal functions
%%====================================================================
%%====================================================================
%% Func: init/1
%% Returns: {ok, State}
%%====================================================================
init (Arg) ->
  {ok, Arg}.


%%====================================================================
%% Func: handle_event/2
%% Returns: {ok, State}
%%====================================================================
handle_event (LogEvent, State) ->
  case LogEvent of
    {LogFormat, LogArgs} ->
      io:format ("[~w] " ++ LogFormat ++ "~n", [State | LogArgs]);
    LogString ->
      io:format ("[~w] ~s~n", [State, LogString])
  end,
  {ok, State}.


%%====================================================================
%% Func: terminate/2
%% Returns: ok
%%====================================================================
terminate (Args, State) -> ok.

%%====================================================================
%% Func: code_change/3
%% Returns: {ok, NewState}
%%====================================================================
code_change(OldVsn, State, _) ->
  {ok, State}.
