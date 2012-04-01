-module(remoteLabelTree).
-compile(export_all).
-include("monitor.hrl").
-include("remoteMachine.hrl").

%%���ģ�����ڹ���label������Ӧ�ĸ���ɾ���ġ��顣
%%  -record(machine_label,{
%%                                   id,
%%                                   name="undefined",  %label name
%%                                   type="nt",         %��ǩ����, ʵ�������ڱ�ǩ
%%                                   index,   %���ü��� 
%%                                   syslabel="false", %% �Ƿ�Ϊϵͳ��ǩ
%%                                   hide="false",     %% �Ƿ�����
%%                                   value="nt"          %% ��ǩֵ
%%                                   parentid="",              %%����id
%%                                   childrenid=[]        %%����id , childrenid=[{id,type}] ,type= "machine" | "label"
%%                                   }).


%%�û���ǩ�� Parentid = UserDefine
create_Label(Parentid,Label)->
    case remoteMachineTag:getTagById(Parentid) of
    {error,Error}-> {error,Error};
    PLabel->
        Maxchild = PLabel#machine_label.maxchild,
        TreeIndex = 
        case string:substr(Maxchild,string:rstr(Maxchild,":")+1,string:len(Maxchild)) of
            []-> PLabel#machine_label.treeindex ++ ":1";
            M-> PLabel#machine_label.treeindex++":"++ integer_to_list(list_to_integer(M)+1)
        end,
        NLabel = Label#machine_label{treeindex=TreeIndex},
        NPLabel = PLabel#machine_label{maxchild=TreeIndex},
        case remoteMachineTag:update_label(NPLabel#machine_label.id,NPLabel) of
            {error,Err}-> {error,Err};
             _-> 
            case  remoteMachineTag:create_label(NLabel) of
            {ok,_}->
                {ok,NLabel#machine_label.id};
            Err->Err
            end
        end
   end.

%%ɾ�����ڵ㣬ͬʱɾ���ӽڵ�
removeLabelNode([])->{ok,"remove label succeed "};
removeLabelNode("[]")->{ok,"ok"};
removeLabelNode(Lid)->
    case remoteMachineTag:getTagById(Lid) of
    {error,Err}->{error,Err};
    L->
        Treeindex = L#machine_label.treeindex,
        Labels = remoteMachineTag:getLabel_likeindex(Treeindex),  %%�ҵ������ӽڵ�
        [ remoteMachineTag:remove_label(CLid#machine_label.id)||CLid<-Labels,string:str(CLid#machine_label.treeindex,L#machine_label.treeindex)=:=1],
        {ok,Lid}
    end.

    
%%���½ڵ�
updateLabelNode([])->{ok,"update sucess"};
updateLabelNode([Label = #machine_label{}|Next])->
    case  remoteMachineTag:update_label(Label#machine_label.id,Label) of
    {ok,_}->    updateLabelNode(Next);
    Err->  Err
    end;
updateLabelNode(_Other)->{error,"unknow format"}.

%%���һ����ǩ�µ��ӱ�ǩ
get_children_label(Lid)->
    case remoteMachineTag:getTagById(Lid) of
    {error,Err}->{error,Err};
    L->
        Treeindex = L#machine_label.treeindex,
        Labels = remoteMachineTag:getLabel_likeindex(Treeindex++":"),  %%�ҵ������ӽڵ�.
        [ Label ||Label<- Labels ,string:str(Label#machine_label.id, L#machine_label.treeindex)=:=1]
    end.
    
%%��ѯһ����ǩ�µ�ȫ���豸.
get_allMachinesbylabel(Ids,Lid, Index, Count, Sort, SortType)->
%%  �����ڵ�
    Labelid = 
        case is_list(Lid) of
        true->  Lid;
        _->atom_to_list(Lid)
        end,
    case remoteMachineTag:getTagById(Labelid) of
    []->[];
    {error,_}->[];
    Label->
        case Label#machine_label.type =/=?SYSTAG_USERDEFINE of
        true->        
            remoteMachineTag:get_Machine_ByTag(Ids,Labelid,Index, Count, Sort, SortType);
        false->
            Treeindex = Label#machine_label.treeindex,
            TLabels =remoteMachineTag:getLabel_likeindex(Treeindex),
            io:format("~n Label:~p, TLabels~p",[Label,TLabels]),
            Labels = [ Label1#machine_label.id ||Label1 <- TLabels,string:str(Label1#machine_label.treeindex,Treeindex)=:=1],
            remoteMachineTag:getMachineByUserDefineTags(Labels,Index, Count, Sort, SortType)
        end
    end. 
    
%%labelid �б�ת�� label�б�
id2Label(Ids)->
    remoteMachineTag:getLabel_byIds(Ids).

label2id([])->[];
label2id([Label|Next])->
    [Label#machine_label.id]++label2id(Next).
%%test()->
%%    writeDefaultTag().

    

    
    