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
    R = lists:flatten([get_exported_tests(Beam) || Beam <- Beams]),
    {ok, R}.

run_tests(Tests, JPid) ->
    eunit:test(Tests, [{report, {erlide_eunit_listener, [{jpid, JPid}]}}]),
    timer:sleep(10000),
    erlang:halt().

%%
%% Local Functions
%%

get_exported_tests(Beam) ->
    {ok, Chunks} = beam_lib:chunks(Beam, [exports]),
    {Module, ExportsList} = Chunks,
    get_tests(ExportsList, Module, Beam, []).

get_tests([], _M, _B, Acc) ->
    Acc;
get_tests([{exports, Exports} | Rest], M, Beam, Acc) ->
    NewAcc = get_tests_aux(Exports, M, Beam, Acc),
    get_tests(Rest, M, Beam, NewAcc).

get_tests_aux([], _M, _B, Acc) ->
    Acc;
get_tests_aux([{F, 0} | Rest], Module, Beam, Acc) ->
    Name = atom_to_list(F),
    case is_generator_name(Name) of
        true ->
            code:load_abs(filename:rootname(Beam)),
            Tests = [{Module, Fun, N} || {N, Fun} <- Module:F()],
            code:delete(Module),
            get_tests_aux(Rest, Module, Beam, [Acc | Tests]);
        false ->
            case is_test_name(Name) of
                true ->
                    get_tests_aux(Rest, Module, Beam, [Acc | [{Module, F}]]);
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
