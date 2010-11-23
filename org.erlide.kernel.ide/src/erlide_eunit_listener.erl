%% This library is free software; you can redistribute it and/or modify
%% it under the terms of the GNU Lesser General Public License as
%% published by the Free Software Foundation; either version 2 of the
%% License, or (at your option) any later version.
%%
%% This library is distributed in the hope that it will be useful, but
%% WITHOUT ANY WARRANTY; without even the implied warranty of
%% MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
%% Lesser General Public License for more details.
%%
%% You should have received a copy of the GNU Lesser General Public
%% License along with this library; if not, write to the Free Software
%% Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
%% USA
%%
%% $Id: $
%%
%% @author Jakob C
%% @see eunit
%% @doc EUnit listener for ErlIDE
%% Based on initial code from Mickaël Rémond and Paul Guyot.

-module(erlide_eunit_listener).

-behaviour(eunit_listener).

-define(NODEBUG, true).
-include_lib("eunit/include/eunit.hrl").
-include_lib("eunit/src/eunit_internal.hrl").

%% called from ErlIDE
-export([start/1]).

%% called from EUnit
-export([init/1, handle_begin/3, handle_end/3, handle_cancel/3, terminate/2]).

-type(chars() :: [char() | any()]). % chars()

-record(testcase,
        {
          name :: chars(),
          description :: chars(),
          result :: ok | {failed, tuple()} | {aborted, tuple()} | {skipped, tuple()},
          time :: integer(),
          output :: binary()
         }).

-record(testsuite,
        {
          name = <<>> :: binary(),
          time = 0 :: integer(),
          output = <<>> :: binary(),
          succeeded = 0 :: integer(),
          failed = 0 :: integer(),
          aborted = 0 :: integer(),
          skipped = 0 :: integer(),
          testcases = [] :: [#testcase{}]
    }).

-record(state, {jpid,
                testsuite = #testsuite{}
               }).

start(Options) ->
    eunit_listener:start(?MODULE, Options).

init(Options) ->
    JPid = proplists:get_value(jpid, Options),
    St = #state{jpid = JPid,
                testsuite = #testsuite{}},
    receive
        {start, _Reference} ->
            St
    end.

terminate({ok, _Data}, St) ->
    reply(St, terminated, {}),
    ok;
terminate({error, Reason}, St) ->
    reply(St, terminated, {Reason}),
    sync_end(error).

sync_end(Result) ->
    receive
        {stop, Reference, ReplyTo} ->
            ReplyTo ! {result, Reference, Result},
            ok
    end.

reply(#state{jpid=JPid}, What, Argument) ->
    JPid ! {What, JPid, Argument}.

handle_begin(group, Data, St) ->
    NewId = proplists:get_value(id, Data),
    case NewId of
        [] ->
            St;
        [_GroupId] ->
            Desc = proplists:get_value(desc, Data),
            TestSuite = St#state.testsuite,
            NewTestSuite = TestSuite#testsuite{name = Desc},
            reply(St, group_begin, NewTestSuite),
            St#state{testsuite=NewTestSuite};
        %% FIXME subgroups
        _ ->
            St
    end;
handle_begin(test, Data, St) ->
    Name = format_name(proplists:get_value(source, Data),
                       proplists:get_value(line, Data)),
    Desc = format_desc(proplists:get_value(desc, Data)),
    TestCase = #testcase{name = Name, description = Desc},
    reply(St, test_begin, {Name, St#state.testsuite#testsuite.name}),
    St.

handle_end(group, Data, St) ->
    %% Retrieve existing test suite:
    case proplists:get_value(id, Data) of
        [] ->
            St;
        [_GroupId|_] ->
            TestSuite = St#state.testsuite,

            %% Update TestSuite data:
            Time = proplists:get_value(time, Data),
            Output = proplists:get_value(output, Data),
            NewTestSuite = TestSuite#testsuite{ time = Time, output = Output },
            reply(St, group_end, NewTestSuite),
            St#state{testsuite=NewTestSuite}
    end;
handle_end(test, Data, St) ->
    %% Retrieve existing test suite:
    TestSuite = St#state.testsuite,

    %% Create test case:
    Name = format_name(proplists:get_value(source, Data),
                       proplists:get_value(line, Data)),
    Desc = format_desc(proplists:get_value(desc, Data)),
    Result = proplists:get_value(status, Data),
    Time = proplists:get_value(time, Data),
    Output = proplists:get_value(output, Data),
    TestCase = #testcase{name = Name, description = Desc, result = Result,
                         time = Time, output = Output},
    reply(St, test_end, TestCase),
    NewTestSuite = add_testcase_to_testsuite(Result, TestCase, TestSuite),
    St#state{testsuite=NewTestSuite}.

handle_cancel(group, _Data, St) ->
    TestSuite = St#state.testsuite,
    reply(St, group_cancel, TestSuite),
    St;
handle_cancel(test, Data, St) ->
    %% Retrieve existing test suite:
    TestSuite = St#state.testsuite,

    %% Create test case:
    Name = format_name(proplists:get_value(source, Data),
                       proplists:get_value(line, Data)),
    Desc = format_desc(proplists:get_value(desc, Data)),
    Reason = proplists:get_value(reason, Data),
    TestCase = #testcase{
      name = Name, description = Desc,
      result = {skipped, Reason}, time = 0,
      output = <<>>},
    reply(St, test_cancel, TestCase),
    NewTestSuite = TestSuite#testsuite{
                     skipped = TestSuite#testsuite.skipped+1,
                     testcases=[TestCase|TestSuite#testsuite.testcases] },
    St#state{testsuite=NewTestSuite}.

format_name({Module, Function, Arity}, Line) ->
    lists:flatten([atom_to_list(Module), ":", atom_to_list(Function), "/",
                   integer_to_list(Arity), "_", integer_to_list(Line)]).
format_desc(undefined) ->
    "";
format_desc(Desc) when is_binary(Desc) ->
    binary_to_list(Desc);
format_desc(Desc) when is_list(Desc) ->
    Desc.

%% Add testcase to testsuite depending on the result of the test.
add_testcase_to_testsuite(ok, TestCaseTmp, TestSuite) ->
    TestCase = TestCaseTmp#testcase{ result = ok },
    TestSuite#testsuite{
      succeeded = TestSuite#testsuite.succeeded+1,
      testcases=[TestCase|TestSuite#testsuite.testcases] };
add_testcase_to_testsuite({error, Exception}, TestCaseTmp, TestSuite) ->
    case Exception of
        {error,{AssertionException,_},_} when
        AssertionException == assertion_failed;
        AssertionException == assertMatch_failed;
        AssertionException == assertEqual_failed;
        AssertionException == assertException_failed;
        AssertionException == assertCmd_failed;
        AssertionException == assertCmdOutput_failed
        ->
            TestCase = TestCaseTmp#testcase{ result = {failed, Exception} },
            TestSuite#testsuite{
              failed = TestSuite#testsuite.failed+1,
              testcases = [TestCase|TestSuite#testsuite.testcases] };
        _ ->
            TestCase = TestCaseTmp#testcase{ result = {aborted, Exception} },
            TestSuite#testsuite{
              aborted = TestSuite#testsuite.aborted+1,
              testcases = [TestCase|TestSuite#testsuite.testcases] }
    end.
