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
    timer:sleep(1000000),
    erlang:halt().

%%
%% Local Functions
%%

get_exported_tests(Beam) ->
    {ok, Chunks} = beam_lib:chunks(Beam, [exports]),
    {Module, ExportsList} = Chunks,
    get_tests(ExportsList, Module).

get_tests([], _M) ->
    [];
get_tests([{exports, Exports} | Rest], M) ->
    Tests = [{M, F, A} || {F, A} <- Exports, lists:suffix("_test_", atom_to_list(F))],
    [Tests | get_tests(Rest, M)].
