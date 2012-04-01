-module(nmap_manage).
-compile(export_all).

start(IPString,N,NmapString,App,User)when is_list(App),is_list(User)->
    DetsStr = App++":"++User,
    pool_manage:do_action(IPString,N,NmapString,DetsStr).

start(IPString, User)->
    App = dbcs_base:get_app(),
    start(IPString, 20, "  -T3 -sS -O -F ", atom_to_list(App), User).

stop()->
    pool_manage :stop_all(),
    process_pool:stop_all().
    
read_all()-> pool_manage:all_result().

%%ǰ���ǵ�ַ�Σ��ڶ�������ʽһ�εĵ�ַ���������������ǣ�nmap���ò�����������APP��user  ���ͬʱɨ�裬��ôӦ�ô��벻ͬ��user������.
%%���ֻ��tcp��ɨ�裬���£�1,2,3 ���԰�һ��ɨ��ĵ�ַ���õĶ�һЩ��������ȽϿ� 10-15֮��ȽϺá�
%%���������udp��ɨ�裬���԰�ÿ�ε�ɨ��ĵ�ַ���õ���һЩ���������һ�� 1-5֮��ȽϺá� 
%% -sS , -sT  tcp��ɨ�� -sU  udp��ɨ�� -O ϵͳɨ�� -F����ɨ��
test1()->
    start("192.168.0.1-255",15," -T4 -F  -O ","localhost","henry"). 
    
test2()->
    start("192.168.0.1-255",5," -T3 -sS  -O ","localhost","henry"). 

test3()->
    start("192.168.0.1-255",20,"  -T3 -sS -O -F " ,"localhost","henry"). 
%%��
test4()->
    start("192.168.0.1-255",20,"  -T5 -sP" ,"localhost","henry"). 
    
test5()->
    start("192.168.0.1-255",20,"  -T5 -sS -O" ,"localhost","henry"). 
    
get_result(User)->
    App = dbcs_base:get_app(),
    process_pool:get("match_user_result", atom_to_list(App)++":"++User).

%%ɨ��Ľ��ȣ�������1.0��ʱ��ɨ�����.
get_rate(User)->
    App = dbcs_base:get_app(),
    N1 = pool_manage:call_n(),
    N2 = length(process_pool:get("match_user",atom_to_list(App)++":"++User)), 
    round(N2/N1*100).
    
get_statestring(User) ->
    Result = get_result(User),
    parse_result(Result, []).
    
parse_result([], Result) ->lists:reverse(Result);
parse_result([F|R], Result) ->
    IP = proplists:get_value(ip, F, "unkown"),
    Ports = proplists:get_value(service, F, []),
    OS = proplists:get_value(os, F, []),
    S = "Host: "++IP++"(" ++ proplists:get_value(type,OS,"") ++ ") port: " ++ integer_to_list(length(Ports)),
    parse_result(R, [S|Result]).
    
    