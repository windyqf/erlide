-module(sms123,[BASE,Monitor,Rule]).
-extends(action).
-compile(export_all).

-include("monitor.hrl").
-include("monitor_template.hrl").
-include("alert.hrl").

-define(HTTP_REQ_TIMEOUT, 20000).


-define(SUPPORT_MAIL,"support@siteview.com").
-define(SALES_MAIL,"sales@siteview.com").
-define(PRODUCT_NAME,"elecc").

-define(LOG_TYPE,"SMS alert sent").
-define(TIMEOUT,5).

new(Monitor,Rule)->
	Obj = action:new(Monitor,Rule),
	{?MODULE,Obj,Monitor,Rule}.
    
get_monitor()->{ok,{monitor,Monitor}}.

get_rule()->{ok,{rule,Rule}}.

execute() ->
    {ok,{_,Params}} = Rule:get_property(action_param), 
	{ok,{_,Enabled}} = Rule:get_property(enabled),
	case Rule:get_property(disabled) of
		{ok,{_,true}}->
			{error,"disabled"};
		_->
			case THIS:check_enabled(Enabled) of
				false ->
					{error,"time_disabled"};
				_->
					sendSMSAlert(Params, true)
			end
	end.

recover(This)->
    {ok,{_,Params}} = Rule:get_property(action_param), 
	sendSMSAlert(Params, false).
 
% sendSMSAlert(Params, IsAlert) ->
    % CATEGORY = case Monitor:get_attribute(?CATEGORY) of 
		% {ok,{_,Category}} -> 
			% atom_to_list(Category);
		% _->
			% ""
	% end,
	% NAME = case Monitor:get_property(?NAME) of
		% {ok,{_,Name}}->
			% Name;
		% _->
			% ""
	% end,
	% STATE_STRING = case Monitor:get_attribute(?STATE_STRING) of
		% {ok,{_,State}}->
			% State;
		% _->
			% ""
	% end, 
    % LocalCode = string:to_lower(platform:getLocalCode()),
    % TemplateFile = if
        % IsAlert ->
            % Params#sms_alert.template;
        % true ->
            % "Recover"
    % end,
    % Msg = THIS:createMessage("templates.sms", TemplateFile), 
    % [Phone0] = Params#sms_alert.phone,
    % Phone01 = string:tokens(Phone0, ","),
    % Phone1 = api_dutytable:get_duty_info(Params#sms_alert.duty,phone),%%�趨���ֻ����� ��ֵ����е��ֻ����� 
    % Phone = Phone01 ++ Phone1,
    % io:format("Phone: ~p~n", [Phone]),
    % case Params#sms_alert.type of
    % "Web" ->        
        % [SMSUrl] = Params#sms_alert.url,
        % [Username] = Params#sms_alert.username,
        % [Password] = Params#sms_alert.password,
        % {{Y,M,D},{H,F,S}} = erlang:localtime(), 
        % if   LocalCode == "utf-8" ->
            % M = Msg;
        % true ->
            % M = iconv:convert(LocalCode,"utf-8",Msg)  
        % end, 
        %% Msg = "alert from siteivew. monitor: " ++ NAME ++" Status: " ++ CATEGORY++" Time: " ++ integer_to_list(Y)++"_"++integer_to_list(M)++"_"++integer_to_list(D)++" "++integer_to_list(H)++ ":"++integer_to_list(F)++":"++integer_to_list(S),                  
        % F1 = 
            % fun(X)->
                % Url = SMSUrl ++ "?user="++ Username++"&pwd="++Password++"&phone="++X++"&msg="++ mochiweb_util:quote_plus(M),         
                % case http:request(get,{Url,[{"te","trailers, deflate;q=0.5"}]},[],[]) of
                % {ok,{{_,Code,_},_,ReturnString}} ->
                    % case Code of   
                    % 200 ->
                        % Index = string:str(ReturnString,"SMS Submit Successfully"), 
                        % if Index > 0 ->
                            % THIS:logAlert(?LOG_TYPE,X,NAME,"SMS Submit Successfully","ok"),
                            % {ok,{sms,Msg}};
                        % true ->
                            % THIS:logAlert(?LOG_TYPE,X,NAME,"fail","fail"), 
                            % {error,{sms,"sand message error"}}
                        % end;                    
                    % _->
                        % THIS:logAlert(?LOG_TYPE,X,NAME,"fail","fail"), 
                        % {error,{sms,"sand message error"}}  
                    % end;  
                % {error, Reason} ->
                    % THIS:logAlert(?LOG_TYPE,X,NAME,"fail","fail"), 
                    % {error,{sms,"fail"}};
                % _ ->
                    % THIS:logAlert(?LOG_TYPE,X,NAME,"fail","fail"), 
                    % {error,{sms,"sand message error"}}            
                % end
            % end,
        % Ret = lists:map(F1,Phone),
        % {ok, {sms, Ret}};
    % "GSM" ->
        % F1 = 
            % fun(X)->
                % io:format("XPhone: ~p~n", [X]),
                % case gsmOperate:sendMessage("COM1",X,Msg) of
                    % 0 ->
                        % THIS:logAlert(?LOG_TYPE,X,NAME,"SMS Submit Successfully","ok"),
                        % {ok,{sms,Msg}}; 
                    % _ ->
                        % THIS:logAlert(?LOG_TYPE,X,NAME,"fail","fail"), 
                        % {error,{sms,"fail"}}
                % end
            % end,
        % Ret = lists:map(F1,Phone),
        % {ok, {sms, Ret}};
    % _ ->
        % THIS:logAlert(?LOG_TYPE,Phone,NAME,"fail","fail"), 
        % {error,{sms,"error"}} 
    % end. 
	
sendSMSAlert(Params, IsAlert) ->
	NAME = case Monitor:get_property(?NAME) of
		{ok,{_,Name}}->
			Name;
		_->
			""
	end,
    TemplateFile = if
        IsAlert ->
            Params#sms_alert.template;
        true ->
            "Recover"
    end,
    Msg = THIS:createMessage("templates.sms", TemplateFile), 
    Phone1 = api_dutytable:get_duty_info(Params#sms_alert.duty,phone),%%�趨���ֻ����� ��ֵ����е��ֻ����� 	
    [Phone2] = Params#sms_alert.other,
	if 
		Phone1 =:= [] -> 
			sendforDuty(Phone2,Params,NAME,Msg),
			sendSMS(Phone2,Params,NAME,Msg,IsAlert,[]);
		Phone1 =/= [] -> 
			sendforDuty(Phone2,Params,NAME,Msg),
			sendSMS(Params#sms_alert.other,Params,NAME,Msg,IsAlert,[])
	end.
			
	 %~ Phone1 = api_dutytable:get_duty_info(Params#sms_alert.phone,phone),%%�趨���ֻ����� ��ֵ����е��ֻ����� 	
  
    %~ if 
	%~ Phone1 =:= [] ->
		%~ Phone2  = Params#sms_alert.other,
		%~ io:format("Phone2 Phone2 ~p~n",[Phone2]),
		%~ sendforDuty(Phone2,Params,NAME,Msg),
		%~ sendSMS(Phone2,Params,NAME,Msg,IsAlert,[]);
	%~ Phone1 =/= [] ->
		%~ io:format("Phone1 Phone1 ~p~n",[Phone1]),
		%~ sendforDuty(Phone1,Params,NAME,Msg),
		%~ sendSMS(Params#sms_alert.phone,Params,NAME,Msg,IsAlert,[])
		%~ sendSMS(Params#sms_alert.phone,Params,NAME,Msg,IsAlert,[])
	%~ end.
	
sendforDuty(Phone,Params,NAME,Msg)->
	SmsEncode = case preferences:get(sms_settings,smsEncode) of
		{ok,[{_,V5}]}->
			V5;
		_->
			"utf-8"
		end,
	MsgP = iconv:convert(httputils:pageEncode(), SmsEncode,Msg),
	{ok,do_sendSMS(Phone,NAME,MsgP,Params,Msg)}.
	

sendSMS([],_,_,_,_,Result)->{ok,Result};	
sendSMS(["other"|T],Params,NAME,Msg,IsAlert,Result)->
	SmsEncode = case preferences:get(sms_settings,smsEncode) of
		{ok,[{_,V5}]}->
			V5;
		_->
			"utf-8"
		end,
	MsgP = iconv:convert(httputils:pageEncode(), SmsEncode,Msg),
	[Other] =  Params#sms_alert.other,
	To = string:tokens(Other,","),
	%~ io:format("sendSMS other To To phone = ~p~n",[To]),
	Ret = do_sendSMS(To,NAME,MsgP,Params,Msg),
    sendSMS(T,Params,NAME,Msg,IsAlert,Result++Ret);
sendSMS([Phone|T],Params,NAME,Msg,IsAlert,Result)->	
	SmsEncode = case preferences:get(sms_settings,smsEncode) of
		{ok,[{_,V5}]}->
			V5;
		_->
			"utf-8"
		end,
	Ret = 
	case preferences:get(additional_sms_settings,list_to_atom(Phone)) of
		{ok,[{_,Ap}|_]}->
			CheckShedule = THIS:check_schedule(Ap#additional_sms_settings.schedule),
			if 
				Ap#additional_sms_settings.disable =/= "true" andalso CheckShedule ->
					NewContent = case Ap#additional_sms_settings.template of
						"use_alert"->
							Msg;
						Temp->
							if
								IsAlert->
									THIS:createMessage("templates.mail",Temp);
								true->
									THIS:createMessage("templates.mail","Recover")
							end
					end,
					MsgP = iconv:convert(httputils:pageEncode(), SmsEncode,NewContent),
					To = [Ap#additional_sms_settings.email],
					io:format("sendSMS Phone To To phone = ~p~n",[To]),
					do_sendSMS(To,NAME,MsgP,Params,NewContent);
					
				true ->
					[{error,Ap#additional_sms_settings.email,"disabled or schedule"}]
			end;
		_->
			[{error,sms,"sms setting not found"}]
	end,
	sendSMS(T,Params,NAME,Msg,IsAlert,Result++Ret).

get_web_sms_config(Key) ->
	Conf = "conf/websms.conf",
	case file:consult(Conf) of
		{ok,Data} ->
			Fun = fun([],_) ->
						case Key of
							user	-> "user";
							pwd		-> "pwd";
							phone	-> "phone";
							msg		-> "msg";
							_		-> "none"
						end;

					([Item|Items],Self) ->
						case Item of
							{Key,Node} -> Node;
							_ -> Self(Items,Self)
						end
				end,
			Fun(Data,Fun);
		_ -> 
			case Key of
				user	-> "user";
				pwd		-> "pwd";
				phone	-> "phone";
				msg		-> "msg";
				_		-> "none"
			end
	end.
	
do_sendSMS(To,NAME,MsgP,Params,Msg)->
	%~ io:format("send sms to:~p~n",[To]),
	case Params#sms_alert.type of
		"Web" ->        
			[SMSUrl] = Params#sms_alert.url,
			[Username] = Params#sms_alert.username,
			[Password] = Params#sms_alert.password,
			Message = mochiweb_util:quote_plus(MsgP),
			inets:start(),
			NewHeaders = [{"Host",get_web_sms_config(host)},{"SOAPAction",get_web_sms_config(soapaction)}],
			ContentType = get_web_sms_config(contenttype),
			Request = getData(To,Username,Password,Message),
			io:format("Request Request ~p~n",[Request]),
			Options = [],
			NewOptions = [{cookies, enabled}|Options],
			httpc:set_options(NewOptions),
			case httpc:request(post,
					       {SMSUrl,NewHeaders,
						ContentType,
						Request},
					       [{timeout,?HTTP_REQ_TIMEOUT}],
					       [{sync, true}, {full_result, true},
						{body_format, string}]) of
				{ok,{{_HTTP,200,_OK},ResponseHeaders,ResponseBody}} ->
				    {ok, 200, ResponseHeaders, ResponseBody},
				    THIS:logAlert(?LOG_TYPE,To,MsgP,"SMS Submit Successfully","ok"),
				    {ok,{To,ResponseBody}};    
				{ok,{{_HTTP,500,_Descr},ResponseHeaders,ResponseBody}} ->
				    {ok, 500, ResponseHeaders, ResponseBody},
				     THIS:logAlert(?LOG_TYPE,To,MsgP,"fail","fail"),
				    {ok,{To,ResponseBody}};    
				{ok,{{_HTTP,ErrorCode,_Descr},ResponseHeaders,ResponseBody}} ->
				    {ok, ErrorCode, ResponseHeaders, ResponseBody},
				    THIS:logAlert(?LOG_TYPE,To,MsgP,"fail","fail"),
				    {ok,{To,ResponseBody}};  
				Other ->
				    Other,
				    {error,{To,"sand message error"}}  
			    end;
		"GSM" ->
			F1 = 
				fun(X)->
					io:format("XPhone: ~p~n", [X]),
					case gsmOperate:sendMessage("COM1",X,MsgP) of
						0 ->
							THIS:logAlert(?LOG_TYPE,X,NAME,"SMS Submit Successfully","ok"),
							{ok,{X,Msg}}; 
						_ ->
							THIS:logAlert(?LOG_TYPE,X,NAME,"fail","fail"), 
							{error,{X,"fail"}}
					end
				end,
			lists:map(F1,To);
		_ ->
			THIS:logAlert(?LOG_TYPE,string:join(To,","),NAME,"fail","fail"), 
			[]
	end.

getData(TO,Username,Password,Message) ->
	"<?xml version=\"1.0\" encoding=\"utf-8\"?>" ++ 
	"<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""++
	" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" ++
	"<soap:Body>" ++ 
	"<SendSMS xmlns=\"http://iamsweb.gmcc.net/WS/\">" ++
	"<systemid>"++
	get_web_sms_config(systemid)++
	"</systemid>" ++
	"<sysAccount>"++
	Username ++
	"</sysAccount>" ++
	"<sysPassword>"++
	Password++
	"</sysPassword>" ++
	"<toUserID>"++
	get_web_sms_config(toUserID)++
	"</toUserID>" ++
	"<toMobile>"++
	TO ++
	"</toMobile>" ++
	"<fromUserID>"++
	get_web_sms_config(fromUserID)++
	"</fromUserID>" ++
	"<fromMobile>"++
	get_web_sms_config(fromMobile)++
	"</fromMobile>" ++
	"<autoForward>"++
	get_web_sms_config(autoForward)++
	"</autoForward>" ++
	"<validdt>"++
	get_web_sms_config(validdt)++
	"</validdt>" ++
	"<allowLock>"++
	get_web_sms_config(allowLock)++
	"</allowLock>" ++
	"<allowAlert>"++
	get_web_sms_config(allowAlert)++
	"</allowAlert>" ++
	"<sourceNo>"++
	get_web_sms_config(sourceNo)++
	"</sourceNo>" ++
	"<content>"++
	Message ++
	"</content>" ++
	"</SendSMS>" ++
	"</soap:Body>" ++
	"</soap:Envelope>".
	
loop(RequestId,Data) ->
    receive
        {http, {RequestId, Result}} ->
                {ok,binary_to_list(Result)};   
        _->
            {error,-1}       
    end.

get_template_file_list()->
	[filename:basename(X)||X<-filelib:wildcard("templates.sms/*"),filelib:is_file(X),not filelib:is_dir(X)].
	
	
read_template_file(Name)->
	case file:read_file("templates.sms/" ++ Name) of
		{ok,Bin}->
			{ok,binary_to_list(Bin)};
		Else->
			Else
	end.

write_template_file(Name,Data)when is_binary(Data)->
	case file:write_file("templates.sms/" ++ Name,Data) of
		ok->
			{ok,Name};
		Else->
			Else
	end;
write_template_file(Name,Data)->
	case file:write_file("templates.sms/" ++ Name,list_to_binary(Data)) of
		ok->
			{ok,Name};
		Else->
			Else
	end.

remove_template_file(Name)->
	case file:delete("templates.sms/" ++ Name) of
		ok->
			{ok,Name};
		Else->
			Else
	end.	

getScalarValues(Prop,Params)->
	case Prop of
		template->
			[{filename:basename(X),filename:basename(X)}||X<-filelib:wildcard("templates.sms/*")];
		phone->
			case preferences:all(additional_sms_settings) of
				{ok,Emails}->
					[{Y#additional_sms_settings.name,atom_to_list(X)}||{X,Y}<-Emails];
				_->
					[]
			end;
		_->
			BASE:getScalarValues(Prop,Params)
	end.

get_template_property()->
	%BASE:get_template_property() ++ 
	[
	#property{name=template,title="Template",type=scalar,description="choose which template to use for formatting the contents of the message.  If you are sending mail to a pager, choose one of the \"short\" templates."}
	].    
    
    