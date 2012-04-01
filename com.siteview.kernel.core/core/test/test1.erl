-module(test1).
-compile(export_all).

-define(PRINT,io:format).

-include("alert.hrl").
-include("monitor.hrl").

test()->
	% �Ҹ��ڵ�
	[R|_] = api_siteview:get_nodes(),
	RId = element(1,R),
	?PRINT("--------TEST START-------------~n"),
	
	% �ڸ��ڵ���������һ����
	{ok,Group}=api_group:create(RId,[{name,"_TEST"},{class,group},{frequency,0},{depends_on,"none"},{depends_condition,"good"}]),
	GId = proplists:get_value(id,Group),
	timer:sleep(1000),
	?PRINT("[OK]-Create Group:~p~n",[GId]),
	
	% ��������Ϣ
	Group2 = lists:keyreplace(name,1,Group,{name,"_TEST_"}),
	{ok,Grp} = api_group:update(Group2),
	timer:sleep(1000),
	?PRINT("[OK]-Update Group:~p~n",[GId]),
	
	% �ٽ�һ����
	{ok,Grp2}=api_group:create(RId,[{name,"_TEST2_"},{class,group},{frequency,0},{depends_on,"none"},{depends_condition,"good"}]),
	GId2 = proplists:get_value(id,Grp2),
	timer:sleep(1000),
	?PRINT("[OK]-Create Group:~p~n",[GId2]),
	
	% ��һ�������
	{ok,M1} = api_monitor:create(GId,[{class,memory_monitor},{name,"Memory"},
												{frequency,600},{disabled,false},{verfiy_error,false},{error_frequency,0},
												{depends_on,"none"},{depends_condition,"good"},{schedule,"all"},{machine,[]}]),
	MId1 = proplists:get_value(id,M1),
	timer:sleep(1000),
	?PRINT("[OK]-Create Monitor:~p~n",[MId1]),
	
	%���¼����
	M2 = lists:keyreplace(name,1,M1,{name,"_TEST_Memory"}),
	{ok,M3} = api_monitor:update(M2),
	timer:sleep(1000),
	?PRINT("[OK]-Update Monitor:~p~n",[MId1]),
	
	%�ƶ������
	{ok,_} = api_monitor:move(MId1,GId2),
	timer:sleep(1000),
	?PRINT("[OK]-Move Monitor(~p) To:~p To:~n",[MId1,GId2]),
	
	%���Ƽ����
	{ok,M4} = api_monitor:copy(MId1,GId),
	MId2 = proplists:get_value(id,M4),
	timer:sleep(1000),
	?PRINT("[OK]-Copy Monitor:~p To:~p,New Monitor:~p~n",[MId1,GId2,MId2]),
	
	% ȡ�������Ϣ
	MInf = api_monitor:info(MId1),
	?PRINT("[OK]-Get Monitor Info:~p ~n",[MInf]),

	% �����������Ϣ
	RInf = api_monitor:get_run_info(MId1),
	?PRINT("[OK]-Get Monitor Run Info:~p ~n",[RInf]),
	
	% �������ֵ
	?PRINT("[OK]-Get Monitor Classifier:~p ~n",[api_monitor:get_classifier(MId1,good)]),
	?PRINT("[OK]-Get Monitor Classifier:~p ~n",[api_monitor:get_classifier(MId1,warning)]),
	?PRINT("[OK]-Get Monitor Classifier:~p ~n",[api_monitor:get_classifier(MId1,error)]),
	?PRINT("[OK]-Get Monitor Classifier:~p ~n",[api_monitor:get_all_classifier(MId2)]),
	
	% ���ü����
	timer:sleep(1000),
	{ok,_}=api_monitor:disable(MId2,""),
	?PRINT("[OK]-Disable Monitor:~p ~n",[MId2]),
	
	% ���ü����
	timer:sleep(1000),
	{ok,_}=api_monitor:enable(MId2),
	?PRINT("[OK]-Enable Monitor:~p ~n",[MId2]),
	
	% ˢ�¼����
	timer:sleep(1000),
	true = api_monitor:refresh(MId1),
	?PRINT("[OK]-Refresh Monitor:~p ~n",[MId1]),
	
	% ȡ������
	timer:sleep(1000),
	Counters = api_monitor:getBrowseData("add",[{class,"browsableNTCounter_monitor"},{machine,""},
									{pFile,"templates.perfmon/browsable/winntsys.xml"}]),
	?PRINT("[OK]-Get Monitor Counters length:~p ~n",[length(Counters)]),
	
	% ȡ��ֵ����
	{ok,StateValue} = api_monitor:getStatePropertyObjects(MId1),
	?PRINT("[OK]-Get Monitor(~p) State Object:~p ~n",[MId1,StateValue]),
	
	% ���
	[_,_|_] = api_monitor:browse('memory_monitor',[]),
	?PRINT("[OK]-Browse Monitor~n"),
	
	% ͳ����Ϣ
	timer:sleep(1000),
	{ok,StatInfo} = api_monitor:get_stat(),
	?PRINT("[OK]-Get Monitor Stat:~p~n",[StatInfo]),
	
	% �������еļ����
	timer:sleep(1000),
	{ok,RunningMonitor}  = api_monitor:get_running(),
	?PRINT("[OK]-Get Monitor Running:~p~n",[RunningMonitor]),
	
	% ������еļ����
	timer:sleep(1000),
	{ok,RecentMonitor}  = api_monitor:get_running(),
	?PRINT("[OK]-Get Monitor Recent:~p~n",[RecentMonitor]),
	
	% ȡ�������־
	{ok,Log} = api_monitor:get_log(erlang:date(),MId1),
	?PRINT("[OK]-Get Monitor(~p) Log:~p~n",[MId1,Log]),
	
	% ���øü�����ĸ澯
	timer:sleep(1000),
	{ok,_} = api_monitor:disable_alert(MId1,"",60),
	?PRINT("[OK]-Disable Monitor(~p) Alert.~n",[MId1]),
	
	% ���ü�����ĸ澯
	timer:sleep(1000),
	{ok,_} = api_monitor:enable_alert(MId1),
	?PRINT("[OK]-Enable Monitor(~p) Alert.~n",[MId1]),
	
	% ȡ����������
	{ok,_} = api_monitor:get_hostname(MId1),
	?PRINT("[OK]-Get Host Name of Monitor(~p).~n",[MId1]),
	
	% ������
	timer:sleep(1000),
	{ok,_} = api_group:disable(GId,""),
	?PRINT("[OK]-Disable Group:~p~n",[GId]),
	
	% ������
	timer:sleep(1000),
	{ok,_} = api_group:enable(GId),
	?PRINT("[OK]-Enable Group:~p~n",[GId]),
	
	% ��������ļ����
	timer:sleep(1000),
	{ok,_} = api_group:disable_monitors(GId,""),
	?PRINT("[OK]-Disable Monitors of Group:~p~n",[GId]),
	
	% ��������ļ����
	timer:sleep(1000),
	{ok,_} = api_group:enable_monitors(GId),
	?PRINT("[OK]-Enable Monitors of Group:~p~n",[GId]),
	
	% ȡ��������
	GName = "_TEST_" = api_siteview:get_object_name(GId),
	?PRINT("[OK]-Get Name Of Group(~p):~p~n",[GId,GName]),
	
	% ȡ����ĸ��׽ڵ��id
	?PRINT("[OK]-Get Parent Id Of Monitor(~p):~p~n",[MId1,api_siteview:get_parent_id(MId1)]),
	
	% ȡ����ĸ��׽ڵ������
	?PRINT("[OK]-Get Parent Name Of Monitor(~p):~p~n",[MId1,api_siteview:get_parent_name(MId1)]),
	
	% ȡ����ȫ����
	?PRINT("[OK]-Get Full Name Of Monitor(~p):~p~n",[MId1,api_siteview:get_full_name(MId1)]),
	
	% ȡ�����·��
	?PRINT("[OK]-Get Path Of Monitor(~p):~p~n",[MId1,api_siteview:get_object_path(MId1)]),
	
	% ȡ���е���ͼ����
	timer:sleep(1000),
	Total =length(api_siteview:getAllGroupsMonitors()),
	?PRINT("[OK]-Get All Groups And Monitors,Total:~p~n",[Total]),
	
	% ȡ���е���
	timer:sleep(1000),
	Total2 =length(api_siteview:getAllGroups()),
	?PRINT("[OK]-Get All Groups,Total:~p~n",[Total2]),
	
	% �����澯
	timer:sleep(1000),
	Target = lists:flatten(io_lib:format("<~w>",[GId])),
	{ok,Alert} = api_alert:create([{name,"Alert_TEST_"},{class,rule},{target,Target},{action,mailto},
								{action_param,#mail_alert{sendto=[],other="xianfang.shi@dragonflow.com",template="Default"}},
								{category,error},{condition,{always,1}},{enabled,true},{name_match,""},
								{status_match,""},{type_match,"any"}]),
	AId = proplists:get_value(id,Alert),
	?PRINT("[OK]-Create Alert:~p~n",[AId]),
	
	%ȡ���и澯
	timer:sleep(1000),
	[_|_] = api_alert:get_all(),
	?PRINT("[OK]-Get All Alert.~n"),
	
	%���¸澯
	timer:sleep(1000),
	Alert2 = lists:keyreplace(name,1,Alert,{name,"_Alert_TEST_"}),
	{ok,_}=api_alert:update(Alert2),
	?PRINT("[OK]-Update Alert:~p.~n",[AId]),
	
	% ȡ�澯�ɱ����ԣ�scalar����ֵ
	timer:sleep(1000),
	AltScalar = api_alert:get_scalar_property(mailto,to,[]),
	?PRINT("[OK]-Get Scalar Property of Alert:~p~n",[AltScalar]),
	
	% �澯����
	timer:sleep(1000),
	{ok,_} = api_alert:alert_test(MId1,AId),
	?PRINT("[OK]-Alert Test:~p,Monitor:~p~n",[AId,MId1]),
	
	% �澯��־
	timer:sleep(1000),
	{ok,AlertLog} = api_alert:get_log(erlang:date()),
	?PRINT("[OK]-Get Alert Log,size:~p~n",[length(AlertLog)]),
	
	% �������и澯
	timer:sleep(1000),
	{ok,_} = api_alert:disable_all(),
	?PRINT("[OK]-Disable All Alerts.~n"),
	
	% �������и澯
	timer:sleep(1000),
	{ok,_} = api_alert:enable_all(),
	?PRINT("[OK]-Enable All Alerts.~n"),
	
	% д�澯ģ��
	{ok,_} = api_alert:write_template_file(mailto,"test","monitor : <name>"),
	?PRINT("[OK]-Write Alert Template.~n"),
	
	% �澯ģ���б�
	?PRINT("[OK]-Get List of Alert Template:~p~n",[api_alert:get_template_file_list(mailto)]),
	
	% ��ȡ�澯ģ��
	{ok,_} = api_alert:read_template_file(mailto,"test"),
	?PRINT("[OK]-Read Alert Template.~n"),
	
	% ɾ���澯ģ��
	{ok,_} = api_alert:remove_template_file(mailto,"test"),
	?PRINT("[OK]-Remove Alert Template.~n"),
	
	% ��ѯ�澯��־
	{eof,_} = api_alert:query_log(sv_datetime:prev_date(erlang:date()),{1,1,1},erlang:date(),{23,59,59},
									[{id,'=',AId}],1,10000),
	?PRINT("[OK]-Query Alert Log:~p.~n",[AId]),
	
	% ɾ���澯
	{ok,_} = api_alert:delete(AId),
	?PRINT("[OK]-Delete Alert:~p.~n",[AId]),
	
	% ȡ��Windows����
	?PRINT("[OK]-Get Windows Machine length:~p.~n",[length(api_machine:get_ntmachine())]),
	
	% ȡ��Unix����
	?PRINT("[OK]-Get Unix Machine length:~p.~n",[length(api_machine:get_unixmachine())]),
	
	% ��������
	{ok,MachId} = api_machine:create_machine(#machine{}),
	?PRINT("[OK]-Create Machine:~p.~n",[MachId]),
	
	% ��������
	{ok,_} = api_machine:update_machine(#machine{id=MachId,name="MACH_TEST"}),
	?PRINT("[OK]-Update Machine:~p.~n",[MachId]),
	
	% ɾ������
	{ok,_} = api_machine:delete_machine(MachId),
	?PRINT("[OK]-Delete Machine:~p.~n",[MachId]),
	
	% ȡ����������б�
	MsetList = api_monitor_set:get_monitorset_list(),
	?PRINT("[OK]-Get List of MonitorSet, length:~p.~n",[length(MsetList)]),
	
	% ȡ�����������Ϣ,���ü�������ϣ��Ӽ��ϴ��������
	case lists:keysearch("PingGroup.mset",1,MsetList) of
		{value,_}->
			{ok,Mset} = api_monitor_set:get_monitorset("PingGroup.mset"),
			?PRINT("[OK]-Get Content of MonitorSet:~p.~n",[Mset]),
			{ok,Mset2} = api_monitor_set:configure_monitorset("PingGroup.mset",[{"%ip%","127.0.0.1"}]),
			?PRINT("[OK]-Configure MonitorSet:~p.~n",[Mset2]),
			MsIds = lists:foldl(fun(X,RR)->
								{ok,Mdata} = api_monitor_set:create_monitor_from_monitorset(GId,X),
								XMId = proplists:get_value(id,Mdata),
								?PRINT("[OK]-Create Monitor from MonitorSet:~p.~n",[XMId]),
								RR ++ [XMId]
							end,[],Mset2#monitor_set.monitors),
			timer:sleep(5000),
			lists:map(fun(X)->
						{ok,_} = monitor_delete(X),
						?PRINT("[OK]-Delete Monitor that Create from MonitorSet:~p.~n",[X])
					end, MsIds),
			ok;
		_->
			?PRINT("[WARN]-MonitorSet 'PingGroup.mset' is missing, MonitorSet test is ignored.")
	end,
	
	% �����ģ���б�
	Mts = api_monitor_template:get_templates(),
	?PRINT("[OK]-Get List of Monitor Template,Length:~p~n",[length(Mts)]),
	
	% ȡ�����ģ������
	lists:map(fun(X)->
			MtPs = api_monitor_template:get_template(element(1,X)),
			?PRINT("[OK]-Get Monitor Template(~p),Find Propertis:~p~n",[element(1,X),length(MtPs)]),
			ok
		end,Mts),
	
	% ȡ�������б�
	Servers = api_monitor_template:get_servers([{"class","service_monitor"}]),
	?PRINT("[OK]-Get Servers,Length:~p~n",[length(Servers)]),
	
	% ȡState����
	api_monitor_template:get_template_state(memory_monitor,[]),
	StProps = api_monitor_template:get_template_state(MId1,memory_monitor,[]),
	?PRINT("[OK]-Get State Property,Length:~p~n",[length(StProps)]),
	
	% ����Schedule 
	{ok,SchId} = api_schedule:create({"","range",{"enabled",[],[]},{"enabled",[],[]},
									{"enabled",[],[]},{"enabled",[],[]},{"enabled",[],[]}
									,{"enabled",[],[]},{"enabled",[],[]}}),
	?PRINT("[OK]-Create New Schedule:~p~n",[SchId]),
	
	% ����Schedule
	ok = api_schedule:update(SchId,{"_","range",{"enabled",[],[]},{"enabled",[],[]},
									{"enabled",[],[]},{"enabled",[],[]},{"enabled",[],[]}
									,{"enabled",[],[]},{"enabled",[],[]}}),
	?PRINT("[OK]-Update Schedule:~p~n",[SchId]),
	
	% ȡSchedule�б�
	[_|_] = api_schedule:get_infos(),
	?PRINT("[OK]-Get List Of Schedule.~n"),
	
	% ȡSchedule��Ϣ
	[_|_] = api_schedule:get_info(SchId),
	?PRINT("[OK]-Get Info Of Schedule.~n"),
	
	% ȡSchedule ����
	{ok,_} = api_schedule:get_schedulename(SchId),
	?PRINT("[OK]-Get Name Of Schedule.~n"),
	
	% ͨ������ȡSchedule��Ϣ
	[_|_] = api_schedule:get_info_by_name("_"),
	?PRINT("[OK]-Get Schedule By Name.~n"),
	
	% Schedule�Ƿ��Ѵ���
	ok =  api_schedule:name_existed("_"),
	?PRINT("[OK]-Schedule Name Existed.~n"),
	
	% Schedule�Ƿ�ʹ����
	SchUsed = api_schedule:schedule_used(SchId),
	?PRINT("[OK]-Schedule is in using:~p.~n",[SchUsed]),
	
	% ɾ��Schedule
	ok = api_schedule:delete(SchId),
	?PRINT("[OK]-Delete Schedule:~p.~n",[SchUsed]),
	
	% ɾ�������
	timer:sleep(1000),
	{ok,_} = monitor_delete(MId1),
	{ok,_} = monitor_delete(MId2),
	?PRINT("[OK]-Delete Monitor:~p,~p~n",[MId1,MId2]),
	
	%ɾ����
	timer:sleep(1000),
	{ok,_} = api_group:delete(GId),
	{ok,_} = api_group:delete(GId2),
	?PRINT("[OK]-Delete Group:~p,~p~n",[GId,GId2]),
	
	?PRINT("--------TEST END-------------~n").
	
monitor_delete(Id)->
	case api_monitor:delete(Id) of
		{error,monitor_is_running}->
			?PRINT("[OK]-Monitor(~p) Is Running,Wait 1 Seconds.~n",[Id]),
			timer:sleep(1000),
			monitor_delete(Id);
		Else->
			Else
	end.
	
t2()->
	Ps = erlang:processes(),
	lists:foldl(fun(X,R)->
				case erlang:process_info(X,status) of
					{_,running}->
						io:format("runing_process:~p,function:~p,init call:~p~n",[X,erlang:process_info(X,current_function),erlang:process_info(X,current_function)]),
						R;
					{_,runnable}->
						io:format("runnable:~p,function:~p~,init call:~p~n",[X,erlang:process_info(X,current_function)]),
						R;
					_->
						pass,
						R
				end
			end,[],Ps),
	ok.