%% Author: jakob
%% Created: 12 nov 2010
%% Description: TODO: Add description to erlide_eunit
-module(erlide_eunit).

%%
%% Include files
%%

%%
%% Exported Functions
%%

-export([find_tests/1, run_tests/2]).

%%
%% API Functions
%%

find_tests(Beams) ->
    R = get_exported_tests(Beams),
    {ok, R}.

run_tests(Tests, JPid) ->
    EUnitTests = get_tests(Tests),
    eunit:test(EUnitTests, [{report, {erlide_eunit_listener, [{jpid, JPid}]}}]),
    timer:sleep(10000),
    erlang:halt().

%%
%% Local Functions
%%

-record(test, {m, f}).
-record(generated, {m, f}).

get_exported_tests(Beams) ->
    get_exported_tests(Beams, []).

get_exported_tests([], Acc) ->
    lists:reverse(Acc);
get_exported_tests([Beam | Rest], Acc) ->
    NewAcc = get_exported_tests_aux(Beam, Acc),
    get_exported_tests(Rest, NewAcc).

get_exported_tests_aux(Beam, Acc) ->
    {ok, Chunks} = beam_lib:chunks(Beam, [exports]),
    {Module, ExportsList} = Chunks,
    get_tests(ExportsList, Module, Beam, Acc).

get_tests([], _M, _B, Acc) ->
    Acc;
get_tests([{exports, Exports} | Rest], Module, Beam, Acc) ->
    NewAcc = get_tests_aux(Exports, Module, Beam, Acc),
    get_tests(Rest, Module, Beam, NewAcc).

get_tests_aux([], _M, _B, Acc) ->
    Acc;
get_tests_aux([{F, 0} | Rest], Module, Beam, Acc) ->
    Name = atom_to_list(F),
    case is_generator_name(Name) of
        true ->
            get_tests_aux(Rest, Module, Beam, [#generated{m=Module, f=F} | Acc]);
        false ->
            case is_test_name(Name) of
                true ->
                    get_tests_aux(Rest, Module, Beam, [#test{m=Module, f=F} | Acc]);
                false ->
                    get_tests_aux(Rest, Module, Beam, Acc)
            end
    end;
get_tests_aux([_ | Rest], Module, Beam, Acc) ->
    get_tests_aux(Rest, Module, Beam, Acc).

is_generator_name(Name) ->
    lists:suffix("_test_", Name).

is_test_name(Name) ->
    lists:suffix("_test", Name).

get_tests(Tests) ->
    get_tests(Tests, []).

get_tests([], Acc) ->
    lists:reverse(Acc);
get_tests([#generated{m=Module, f=F} | Rest], Acc) ->
    get_tests(Rest, (lists:reverse(Module:F(), Acc)));
get_tests([Test | Rest], Acc) ->
    get_tests(Rest, [Test | Acc]).
