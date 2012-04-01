%%
%% %CopyrightBegin%
%% 
%% Copyright Ericsson AB 2008-2009. All Rights Reserved.
%% 
%% The contents of this file are subject to the Erlang Public License,
%% Version 1.1, (the "License"); you may not use this file except in
%% compliance with the License. You should have received a copy of the
%% Erlang Public License along with this software. If not, it can be
%% retrieved online at http://www.erlang.org/.
%% 
%% Software distributed under the License is distributed on an "AS IS"
%% basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
%% the License for the specific language governing rights and limitations
%% under the License.
%% 
%% %CopyrightEnd%

%% 
%% @doc egd_render 
%%

-module(egd_render).

-export([binary/1, binary/2]).
-export([eps/1]).
-compile(inline).

-include("egd.hrl").
-define('DummyC',0).

binary(Image) -> binary(Image, opaque).

binary(Image, Type) ->
    parallel_binary(precompile(Image),Type).

parallel_binary(Image = #image{ height = Height },Type) ->
    case lists:min([erlang:system_info(schedulers), Height]) of
        1 ->
	    % if the height or the number of schedulers is 1
	    % do the scanlines in this process.
	    W  = Image#image.width,
	    Bg = Image#image.background,
	    Os = Image#image.objects,
	    erlang:list_to_binary(lists:map(fun
	    	(Y) -> scanline(Y, Os, {0,0,W - 1, Bg}, Type)
	    end, lists:seq(1, Height)));
	Np ->
	    Pids    = start_workers(Np, Type),
    	    Handler = handle_workers(Height, Pids),
    	    init_workers(Image, Handler, Pids),
	    Res = receive_binaries(Height),
    	    finish_workers(Pids),
	    Res
    end.

start_workers(Np, Type) -> start_workers(Np, Type, []).
start_workers( 0,    _, Pids) -> Pids;
start_workers(Np, Type, Pids) when Np > 0 -> 
    start_workers(Np - 1, Type, [spawn_link(fun() -> worker(Type) end)|Pids]).

worker(Type) ->
    receive
	{Pid, data, #image{ objects = Os, width = W, background = Bg }} -> 
	    worker(Os, W, Bg, Type, Pid)
    end.

worker(Objects, Width, Bg, Type, Collector) ->
    receive
    	{Pid, scan, {Ys, Ye}} ->
	    lists:foreach(fun
		(Y) ->
		    Bin = erlang:list_to_binary(scanline(Y, Objects, {0,0,Width - 1, Bg}, Type)),
		    Collector ! {scan, Y, Bin}
		end, lists:seq(Ys,Ye)),
	    Pid ! {self(), scan_complete},
	    worker(Objects, Width, Bg, Type, Collector);
    	{Pid, scan, Y} ->
	    Bin = erlang:list_to_binary(scanline(Y, Objects, {0,0,Width - 1, Bg}, Type)),
	    Collector ! {scan, Y, Bin},
	    Pid ! {self(), scan_complete},
	    worker(Objects, Width, Bg, Type, Collector);
	{_, done} ->
	 ok
    end.

init_workers(_Image, _Handler, []) -> ok;
init_workers(Image, Handler, [Pid|Pids]) ->
    Pid ! {self(), data, Image}, 
    Handler ! {Pid, scan_complete},
    init_workers(Image, Handler, Pids).

handle_workers(H, Pids) -> spawn_link(fun() -> handle_workers(H, H, length(Pids)) end).
handle_workers(_, 0, _) -> ok;
handle_workers(H, Hi, Np) when H > 0 ->
    N = trunc(Hi/(2*Np)),
    receive 
	{Pid, scan_complete} -> 
	    if N < 2 ->
	    	Pid ! {self(), scan, Hi},
		handle_workers(H, Hi - 1, Np);
	    true ->
	    	Pid ! {self(), scan, {Hi - N, Hi}},
	   	handle_workers(H, Hi - 1 - N, Np)
	end
    end.

finish_workers([]) -> ok;
finish_workers([Pid|Pids]) ->
    Pid ! {self(), done},
    finish_workers(Pids).

receive_binaries(H) -> receive_binaries(H, []).
receive_binaries(0, Bins) -> erlang:list_to_binary(Bins);
receive_binaries(H, Bins) when H > 0 ->
    receive
        {scan, H, Bin} -> 
	    receive_binaries(H - 1, [Bin|Bins])
    end.


scanline(Y, Os, {_,_,Width,_}=LSB, Type) ->
    OLSs  = parse_objects_on_line(Y-1, Width, Os),
    URLSs = resulting_line_spans([LSB|OLSs],Type),

    % FIXME: Can we keep the list sorted instead of sorting it?
    % sort descending
    RLSs = lists:reverse(URLSs),

    resulting_scanline(RLSs,Width).

resulting_scanline(RLSs, Width) -> resulting_scanline(RLSs, Width, []).
resulting_scanline([], _, Scanlines) -> Scanlines;
resulting_scanline([{_,Xl, Xr, C} | RLSs], Width, Scanlines) ->
    {R,G,B,_} = rgb_float2byte(C),
    Scanline = lists:duplicate(trunc(Xr - Xl + 1), <<R:8,G:8,B:8>>),
    resulting_scanline(RLSs, Width, [Scanline|Scanlines]).

resulting_line_spans(LSs,Type) ->
    %% Build a list of "transitions" from left to right.
    Trans = line_spans_to_trans(LSs),
    %% Convert list of "transitions" to linespans.
	trans_to_line_spans(Trans,Type).

line_spans_to_trans(LSs) ->
    line_spans_to_trans(LSs,[],0).

line_spans_to_trans([],Db,_) ->
    lists:sort(Db);
line_spans_to_trans([{_,L,R,C}|LSs],Db,Z) ->
    line_spans_to_trans(LSs,[{{L,Z,start},C},{{R+1,Z,stop},C}|Db],Z+1).

trans_to_line_spans(Trans,Type) ->
    trans_to_line_spans(simplify_trans(Trans,Type,[],{0.0,0.0,0.0,0.0},[])).

trans_to_line_spans(SimpleTrans) ->
    trans_to_line_spans1(SimpleTrans,[]).

trans_to_line_spans1([],Spans) ->
    Spans;
trans_to_line_spans1([_],Spans) ->
    Spans;
trans_to_line_spans1([{L1,_},{L2,C2}|SimpleTrans],Spans) ->
    %% We are going backwards now...
    trans_to_line_spans1([{L2,C2}|SimpleTrans],[{?DummyC,L2,L1-1,C2}|Spans]).

simplify_trans([],_,_,_,Acc) ->
    Acc;
simplify_trans([{{L,_,_},_}|_] = Trans,Type,Layers,OldC,Acc) ->
    {NextTrans,RestTrans} =
	lists:splitwith(fun({{L1,_,_},_}) when L1 == L ->
				true;
			   (_) ->
				false
			end, Trans),
    {C,NewLayers} = color(NextTrans,Layers,Type,OldC),
    case OldC of
        C -> %% No change in color, so transition unnecessary.
            simplify_trans(RestTrans,Type,NewLayers,OldC,Acc);
        _ ->
            simplify_trans(RestTrans,Type,NewLayers,C,[{L,C}|Acc])
    end.

color(Trans,Layers,Type,OldC) ->
    case modify_layers(Layers,Trans) of
        Layers ->
            {OldC,Layers};
        NewLayers ->
            {color(NewLayers,Type),NewLayers}
    end.

color([],_) -> {0.0,0.0,0.0,0.0};
color([{_,C}|_],opaque) -> C;    
color(Layers,alpha) -> color1({0,0,0,0},Layers).

color1(Color,[]) -> Color;
color1(Color,[{_,C}|Layers]) -> color1(blend(Color,C),Layers).

blend(C1,C2) -> alpha_blend(C1,C2).

modify_layers(Layers,[]) -> Layers;
modify_layers(Layers,[{{_,Z,Op},C}|Trans]) ->
    modify_layers(case Op of
		      start ->
			  add_layer(Layers,Z,C);
		      stop ->
			  remove_layer(Layers,Z,C)
		  end,
		  Trans).

add_layer([{Z1,_}=H|Layers],Z,C) when Z1 > Z ->
    [H|add_layer(Layers,Z,C)];
add_layer(Layers,Z,C) ->
    [{Z,C}|Layers].

remove_layer(Layers,Z,C) ->
    Layers -- [{Z,C}].

alpha_blend({R1,G1,B1,A1}, {R2,G2,B2,A2}) ->
  Beta = A2*(1.0 - A1),
  A = A1 + Beta,
  R = R1*A1 + R2*Beta,
  G = G1*A1 + G2*Beta,
  B = B1*A1 + B2*Beta,
  {R,G,B,A}.

parse_objects_on_line(Y, Width, Objects) ->
    parse_objects_on_line(Y, 1, Width, Objects, []).
parse_objects_on_line(_Y, _Z, _, [], Out) -> lists:flatten(Out);
parse_objects_on_line(Y, Z, Width, [O|Os], Out) ->
    case is_object_on_line(Y, O) of
    	false ->
	    parse_objects_on_line(Y, Z + 1, Width, Os, Out);
	true ->
	    OLs = object_line_data(Y, Z, O),
	    TOLs = trim_object_line_data(OLs, Width),
	    parse_objects_on_line(Y, Z + 1, Width, Os, [TOLs|Out])
    end.

trim_object_line_data(OLs, Width) ->
    trim_object_line_data(OLs, Width, []).
trim_object_line_data([], _, Out) -> Out;
trim_object_line_data([{Z, Xl, Xr, C}|OLs], Width, Out) ->
    if 
	Xl > Width ->
            trim_object_line_data(OLs, Width, Out);
	Xr < 0 ->
            trim_object_line_data(OLs, Width, Out);
	true ->
           trim_object_line_data(OLs, Width, [{Z, lists:max([0,Xl]), lists:min([Xr,Width]), C}|Out])
    end.

% object_line_data
% In:
%	Y :: index of height
%	Z :: index of depth
%	Object :: image_object()
% Out:
%	OLs = [{Z, Xl, Xr, Color}]
%	Z = index of height
%	Xl = left X index
%	Xr = right X index 
% Purpose:
%	Calculate the length (start and finish index) of an objects horizontal
%	line given the height index.

object_line_data(Y, Z, Object) -> object_line_data(Y, Z, Object, Object#image_object.type).
object_line_data(Y, Z, #image_object{ span = {X0, Y0, X1, Y1}, color = C}, rectangle) ->
    if
	Y0 =:= Y ; Y1 =:= Y ->
    	    [{Z, X0, X1, C}];
	true ->
    	    [{Z, X0, X0, C},
	     {Z, X1, X1, C}]
    end;

object_line_data(_Y, Z, #image_object{ span = {X0, _, X1, _}, color = C}, filled_rectangle) ->
    [{Z, X0, X1, C}];

object_line_data(Y, Z, #image_object{ span = {X0,Y0,X1,Y1}, color = C}, filled_ellipse) ->
    if 
    	X1 - X0 == 0 -> % if the width is exactly one pixel
	    [{Z, X1, X0, C}];
	X1 - X0 < 0 -> throw(bad_ellipse_width);
	Y1 - Y0 == 0 -> % Height exactly one pixel, get width
	    [{Z, X0, X1, C}];
	true ->
	    Xr = (X1 - X0)/2,
	    Yr = (Y1 - Y0)/2,
	    Yo = trunc(Y - Y0 - Yr),
	    Yo2 = Yo*Yo,
	    Yr2 = Yr*Yr,
	    Xo = math:sqrt((1 - Yo2/Yr2))*Xr,
	    [{Z, round(X0 - Xo + Xr), round(X0 + Xo + Xr), C}]
    end;

object_line_data(Y, Z, #image_object{ intervals = Is, color = C}, filled_triangle) ->
    case lists:keysearch(Y, 1, Is) of
   	{value, {Y, Xl, Xr}} -> [{Z, Xl, Xr, C}];
	false -> []
    end;    

object_line_data(Y, Z, #image_object{ intervals = Is, color = C, thin_color = C2}, thickline) ->
    case dict:find(Y, Is) of
	%{ok, {Xl, Xr}} -> [{Z, Xl, Xr, C}];
	{ok, Ls} ->
		get_line_data(Ls, dict:find(Y-1, Is), dict:find(Y, Is), dict:find(Y+1, Is), Y, Z, C, C2);
	_ -> []
    end;

object_line_data(Y, Z, #image_object{ intervals = Is, color = C}, line) ->
    case dict:find(Y, Is) of
	%{ok, {Xl, Xr}} -> [{Z, Xl, Xr, C}];
	{ok, Ls} -> [{Z, Xl, Xr, C}||{Xl,Xr} <- Ls];
	_ -> []
    end;

object_line_data(Y, Z, O, polygon) ->
    Is = lists:filter(
	fun({Yp,_,_}) ->
	    if Yp == Y -> true; true -> false end
    	end, O#image_object.intervals),
    [	{Z, Xl, Xr, O#image_object.color} || {_, Xl, Xr} <- Is];

object_line_data(Y, Z, #image_object{ color = C, intervals = Is }, text_horizontal) ->
    % FIXME: optimize!
    lists:foldl(
	fun ({Yg,Xl,Xr}, Out) ->
	    if 
		Yg == Y ->
		    [{Z, Xl, Xr, C}|Out];
		true ->
		    Out
	    end
	end, [], Is);
object_line_data(_, Z, #image_object{ span = {X0,_,X1,_}, color = C}, _) ->
    % faked
    [{Z, X0, X1, C}].

get_line_data(Ls, InLsD, InLs0, InLsU, Y, Z, C, C2)->
	LsD= case InLsD of
		{ok,LD} -> LD;
		_ -> []
	end,
	Ls0= case InLs0 of
		{ok,L0} -> L0;
		_ -> []
	end,	
	LsU= case InLsU of
		{ok,LU} -> LU;
		_ -> []
	end,		
	get_line_data(Ls, LsD, Ls0, LsU, Y, Z, C, C2, []).


get_line_data([], _, _, _, _, _, _, _, Ret)->
	lists:reverse(Ret);
get_line_data([{Xl,Xr}|T], LsD, Ls0, LsU, Y, Z, C, {C2Middle,C2}, Ret)->
 	case set_thin_color(Xl, Xr, LsD, Ls0, LsU) of
		null ->
			get_line_data(T, LsD, Ls0, LsU, Y, Z, C, {C2Middle,C2}, [{Z, Xl, Xr, C}|Ret]);
		left ->
%% 			io:format("thin color   Y:~p  Xl:~p  Xr:~p ~n LsU:~p~n LsD:~p~n",[Y, Xl, Xr, LsU, LsD]),
			get_line_data(T, LsD, Ls0, LsU, Y, Z, C, {C2Middle,C2}, [{Z, Xl, Xl, C2Middle}|[{Z, Xl+1, Xr, C}|Ret]] );	
		right ->
%% 			io:format("thin color   Y:~p  Xl:~p  Xr:~p ~n LsU:~p~n LsD:~p~n",[Y, Xl, Xr, LsU, LsD]),
			get_line_data(T, LsD, Ls0, LsU, Y, Z, C, {C2Middle,C2}, [{Z, Xr, Xr, C2Middle}|[{Z, Xl, Xr-1, C}|Ret]] )	
	end.


set_thin_color(Xl, Xr, LsD, Ls0, LsU)->
	L1= check_positon_within_range(Xl, LsD)=:=0 andalso check_positon_within_range(Xr, LsD)=/=0 ,
	L2= check_positon_within_range(Xl, LsU)=:=0 andalso check_positon_within_range(Xr, LsU)=/=0 ,
	L3= check_positon_within_range(Xl-1, Ls0)=:=0 andalso check_positon_within_range(Xl, Ls0)=/=0 andalso check_positon_within_range(Xl+1, Ls0)=/=0 andalso check_positon_within_range(Xl, LsD)=:=0 ,%% andalso check_positon_within_range(Xl, LsD)=/=0  
	
	R1= check_positon_within_range(Xr, LsU)=:=0 andalso check_positon_within_range(Xl, LsU)=/=0 ,
	R2= check_positon_within_range(Xr, LsD)=:=0 andalso check_positon_within_range(Xl, LsD)=/=0 ,
	R3= check_positon_within_range(Xr+1, Ls0)=:=0 andalso check_positon_within_range(Xr, Ls0)=/=0 andalso check_positon_within_range(Xr-1, Ls0)=/=0  andalso check_positon_within_range(Xr, LsU)=:=0 ,%% andalso check_positon_within_range(Xr, LsD)=/=0  
	
	if 	L1 -> left;
		L2 -> left;
		
		R1 -> right;
		R2 -> right;
		R3 -> right;
		
		true->	null
	end.
		

check_positon_within_range(X,Ls)->
	check_positon_within_range(X,Ls,0).

check_positon_within_range(X, [], Ret)->
	Ret;
check_positon_within_range(X, [{Xl,Xr}|T], Ret)->
	case Xl=<X andalso X=<Xr of
		true -> Ret+1;
		_ -> Ret
	end.


is_object_on_line(Y, Object) ->
    is_object_bounds_on_line(Y, Object#image_object.span). 
    
is_object_bounds_on_line(Y, {_,Y0,_,Y1}) ->
    if 
    	Y < Y0 -> false;
	Y > Y1 -> false;
	true -> true
    end.

rgb_float2byte({R,G,B,A}) ->
    {trunc(R*255), trunc(G*255), trunc(B*255), trunc(A*255)}.

%%% primitives to line_spans

%% compile objects to linespans

precompile(Image = #image{ objects = Os }) ->
    Image#image{ objects = precompile_objects(Os) }.

precompile_objects(Os) -> precompile_objects(Os, []).
precompile_objects([], Out) -> lists:reverse(Out);

precompile_objects([O = #image_object{ type = thickline, thin_color = ThinColor,points = [P0,P1] }| Os], Out) ->
    precompile_objects(Os, [O#image_object{ intervals = ls_list2dict(line_ls(ThinColor, P0,P1)) } | Out]);

precompile_objects([O = #image_object{ type = line, points = [P0,P1] }| Os], Out) ->
    precompile_objects(Os, [O#image_object{ intervals = ls_list2dict(line_ls(P0,P1)) } | Out]);
        
precompile_objects([O = #image_object{ type = filled_triangle, points = [P0,P1,P2] } | Os], Out) ->
    precompile_objects(Os, [O#image_object{ intervals = triangle_ls(P0,P1,P2) } | Out]);
    
precompile_objects([O = #image_object{ type = polygon, points = Pts } | Os], Out) ->
    precompile_objects(Os, [O#image_object{ intervals = polygon_ls(Pts) } | Out]);
    
precompile_objects([O = #image_object{ type = arc, points = [P0,P1], internals = D }| Os], Out) ->
    Es = egd_primitives:arc_to_edges(P0, P1, D),
    Ls = lists:foldl(fun
    	({Ep0, Ep1}, D0) ->
	    ls_list2dict(line_ls(Ep0, Ep1), D0)
    end, dict:new(), Es),
    precompile_objects(Os, [O#image_object{ type = line, intervals = Ls } | Out]);

precompile_objects([O = #image_object{ type = text_horizontal, points = [P0], internals = {Font, Text}} | Os], Out) ->
    precompile_objects(Os, [O#image_object{ intervals = text_horizontal_ls(P0, Font, Text) } | Out]);
    
precompile_objects([O|Os], Out) ->
    precompile_objects(Os, [O|Out]).

% triangle 

triangle_ls(P1,P2,P3) ->
    % Find top point (or left most top point),
    % From that point, two lines will be drawn to the 
    % other points.
    % For each Y step, 
  	% bresenham_line_interval for each of the two lines
	% Find the left most and the right most for those lines
    % At an end point, a new line to the point already being drawn
    % repeat same procedure as above
    [Sp1, Sp2, Sp3] = tri_pt_ysort([P1,P2,P3]),   
    triangle_ls_lp(tri_ls_ysort(line_ls(Sp1,Sp2)), Sp2, tri_ls_ysort(line_ls(Sp1,Sp3)), Sp3, []).

% There will be Y mismatches between the two lists since bresenham is not perfect.
% I can be remedied with checking intervals this could however be costly and
% it may not be necessary, depending on how exact we need the points to be.
% It should at most differ by one and endpoints should be fine.

triangle_ls_lp([],_,[],_,Out) -> Out;
triangle_ls_lp(LSs1, P1, [], P2, Out) -> 
    SLSs = tri_ls_ysort(line_ls(P2,P1)),
    N2 = length(SLSs),
    N1 = length(LSs1),
    if 
	N1 > N2 ->
	    [_|ILSs] = LSs1,
    	    triangle_ls_lp(ILSs, SLSs, Out);
	N2 > N1 ->
	    [_|ILSs] = SLSs,
    	    triangle_ls_lp(LSs1, ILSs, Out);
	true ->
    	    triangle_ls_lp(LSs1, SLSs, Out)
    end;
triangle_ls_lp([], P1, LSs2, P2, Out) ->
    SLSs = tri_ls_ysort(line_ls(P1,P2)),
    N1 = length(SLSs),
    N2 = length(LSs2),
    if 
	N1 > N2 ->
	    [_|ILSs] = SLSs,
    	    triangle_ls_lp(ILSs, LSs2, Out);
	N2 > N1 ->
	    [_|ILSs] = LSs2,
    	    triangle_ls_lp(SLSs, ILSs, Out);
	true ->
	    triangle_ls_lp(SLSs, LSs2, Out)
    end;
triangle_ls_lp([LS1|LSs1],P1,[LS2|LSs2],P2, Out) ->
    {Y, Xl1, Xr1} = LS1,
    {_, Xl2, Xr2} = LS2,
    Xr = lists:max([Xl1,Xr1,Xl2,Xr2]),
    Xl = lists:min([Xl1,Xr1,Xl2,Xr2]),
    triangle_ls_lp(LSs1,P1, LSs2, P2, [{Y,Xl,Xr}|Out]).    

triangle_ls_lp([],[],Out) -> Out;
triangle_ls_lp([],_,Out) -> Out;
triangle_ls_lp(_,[],Out) -> Out;
triangle_ls_lp([LS1|LSs1], [LS2|LSs2], Out) ->
    {Y, Xl1, Xr1} = LS1,
    {_, Xl2, Xr2} = LS2,
    Xr = lists:max([Xl1,Xr1,Xl2,Xr2]),
    Xl = lists:min([Xl1,Xr1,Xl2,Xr2]),
    triangle_ls_lp(LSs1, LSs2, [{Y,Xl,Xr}|Out]).    
       
tri_pt_ysort(Pts) ->
    % {X,Y}
    lists:sort(
	fun ({_,Y1},{_,Y2}) ->
	   if Y1 > Y2 -> false; true -> true end
	end, Pts).

tri_ls_ysort(LSs) ->
    % {Y, Xl, Xr}
    lists:sort(
	fun ({Y1,_,_},{Y2,_,_}) ->
	   if Y1 > Y2 -> false; true -> true end
	end, LSs).

% polygon_ls
% In:
%	Pts :: [{X,Y}]
% Out:
% 	LSs :: [{Y,Xl,Xr}]
% Purpose:
%	Make polygon line spans
% Algorithm:
%	1. Find the left most (lm) point
%	2. Find the two points adjacent to that point
%		The tripplet will make a triangle
%	3. Ensure no points lies within the triangle
%	4a.No points within triangle, 
%		make triangle,
%	   	remove lm point
%		1.
%	4b.point(s) within triangle,
%				


polygon_ls(Pts) ->
    % Make triangles
    Tris = polygon_tri(Pts),
    % interval triangles
    lists:flatten(polygon_tri_ls(Tris, [])).

polygon_tri_ls([], Out) -> Out;
polygon_tri_ls([{P1,P2,P3}|Tris], Out) ->
    polygon_tri_ls(Tris, [triangle_ls(P1,P2,P3)|Out]).

polygon_tri(Pts) ->
    polygon_tri(polygon_lm_pt(Pts), []).


polygon_tri([P1,P2,P3],Tris) -> [{P1,P2,P3}|Tris];
polygon_tri([P2,P1,P3|Pts], Tris) ->
    case polygon_tri_test(P1,P2,P3,Pts) of
	false -> polygon_tri(polygon_lm_pt([P2,P3|Pts]), [{P1,P2,P3}|Tris]);
	[LmPt|Ptsn] -> polygon_tri([P2,P1,LmPt,P3|Ptsn], Tris)
    end.

polygon_tri_test(P1,P2,P3, Pts) ->
    polygon_tri_test(P1,P2,P3, Pts, []).
    
polygon_tri_test(_,_,_, [], _) -> false;
polygon_tri_test(P1,P2,P3,[Pt|Pts], Ptsr) ->
    case point_inside_triangle(Pt, P1,P2,P3) of
    	false -> polygon_tri_test(P1,P2,P3, Pts, [Pt|Ptsr]);
    	true -> [Pt|Pts] ++ lists:reverse(Ptsr) 
    end.

% polygon_lm_pt
% In:
%	Pts ::  [{X,Y}]
% Out
%	LmPts = [{X0,Y0},{Xmin,Y0},{X1,Y1},...]
% Purpose:
%	 The order of the list is important
%	 rotate the elements until Xmin is first
%	 This is not extremly fast.

polygon_lm_pt(Pts) ->
    Xs = [X||{X,_}<-Pts],
    polygon_lm_pt(Pts, lists:min(Xs), []).

polygon_lm_pt([Pt0,{X,_}=Ptm | Pts], Xmin, Ptsr) when X > Xmin ->
    polygon_lm_pt([Ptm|Pts], Xmin, [Pt0|Ptsr]);
polygon_lm_pt(Pts, _,  Ptsr) ->
    Pts ++ lists:reverse(Ptsr).


% return true if P is inside triangle (p1,p2,p3),
% otherwise false.

points_same_side({P1x,P1y}, {P2x,P2y}, {L1x,L1y}, {L2x,L2y}) ->
    ((P1x - L1x)*(L2y - L1y) - (L2x - L1x)*(P1y - L1y) *
     (P2x - L1x)*(L2y - L1y) - (L2x - L1x)*(P2y - L1y)) >= 0.

point_inside_triangle(P, P1, P2, P3) ->
    points_same_side(P, P1, P2, P3) and 
    points_same_side(P, P2, P1, P3) and 
    points_same_side(P, P3, P1, P2).
   
%% [{Y, Xl, Xr}]
ls_list2dict(List) -> ls_list2dict(List, dict:new()).
ls_list2dict([], D) -> D;
ls_list2dict([{Y, Xl, Xr}|Ls], D) ->
    case dict:is_key(Y, D) of
        false -> ls_list2dict(Ls, dict:store(Y, [{Xl, Xr}], D));
	true  -> ls_list2dict(Ls, dict:append(Y, {Xl, Xr}, D))
    end.

%% line_ls
%% In:
%%	P1 :: point()
%%	P2 :: point()
%% Out:
%%	{{Ymin,Ymax}, LSD :: line_step_data()}
%% Purpose:
%% 	Instead of points -> intervals


line_ls({Xi0, Yi0},{Xi1,Yi1}) ->
	line_ls(undefined, {Xi0, Yi0},{Xi1,Yi1}).

line_ls(ThinColor, {Xi0, Yi0},{Xi1,Yi1}) ->
    % swap X with Y if line is steep
    Steep = abs(Yi1 - Yi0) > abs(Xi1 - Xi0),

    {Xs0, Ys0, Xs1, Ys1} = case Steep of
	true -> {Yi0,Xi0,Yi1,Xi1};
	false -> {Xi0,Yi0,Xi1,Yi1}
    end,

    {X0,Y0,X1,Y1} = case Xs0 > Xs1 of
	true -> {Xs1,Ys1,Xs0,Ys0};
	false -> {Xs0,Ys0,Xs1,Ys1}
    end,

    DX = X1 - X0,
    DY = abs(Y1 - Y0),

    Error = -DX/2,

    Ystep = case Y0 < Y1 of
	true -> 1;
	false -> -1
    end, 
    case Steep of
	false ->
		case ThinColor of
			undefined ->
				line_ls_step_not_steep({X0, X1},Y0, DX, DY, Ystep, Error, X0, []);
			_ ->
				thick_line_ls_step_not_steep({X0, X1},Y0, DX, DY, Ystep, Error, X0, [])
		end;
	true -> 
		case ThinColor of
			undefined ->
	    		line_ls_step_steep({X0, X1},Y0, DX, DY, Ystep, Error, X0, []);
			_ ->
				thick_line_ls_step_steep({X0, X1},Y0, DX, DY, Ystep, Error, X0, [])
		end
    end.
	 

%% line_ls_step_(not)_steep
%% In:
%% Out:
%%	[{Yi, Xl,Xr}]
%% Purpose:
%% 	Produce an line_interval for each Yi (Y index)	

% Iterating the X-axis

line_ls_step_not_steep({X,X1},Y,Dx,Dy,Ys,E, X0, LSs) when X < X1 ->
    case E >= 0 of
	true ->
	    line_ls_step_not_steep({X+1,X1},Y+Ys,Dx,Dy,Ys, E - Dx + Dy, X+1,[{Y,X0,X}|LSs]);
	false ->
	    line_ls_step_not_steep({X+1,X1},Y,Dx,Dy,Ys, E + Dy, X0, LSs)
    end;
line_ls_step_not_steep({X,_},Y,_Dx,_Dy,_Ystep,_E,X0,LSs) ->
    [{Y,X0,X}|LSs].

% Iterating the Y-axis
line_ls_step_steep({X,X1},Y,Dx,Dy,Ystep,E, X0, LSs) when X =< X1 ->
    case E >= 0 of
	true ->
	    line_ls_step_steep({X + 1,X1},Y+Ystep,Dx,Dy,Ystep,E - Dx + Dy,X,[{X,Y,Y}|LSs]);
	false ->
	    line_ls_step_steep({X + 1,X1},Y,Dx,Dy,Ystep,E + Dy,X0, [{X,Y,Y}|LSs])
    end;
line_ls_step_steep({_X,_},_Y,_Dx,_Dy,_Ystep,_E,_X0,LSs) -> 
    LSs.


thick_line_ls_step_not_steep({X,X1},Y,Dx,Dy,Ys,E, X0, LSs) when X < X1 ->
    case E >= 0 of
	true ->
	    thick_line_ls_step_not_steep({X+1,X1},Y+Ys,Dx,Dy,Ys, E - Dx + Dy, X+1,[{Y+1,X0,X}|[{Y,X0,X}|LSs]]);
	false ->
	    thick_line_ls_step_not_steep({X+1,X1},Y,Dx,Dy,Ys, E + Dy, X0, LSs)
    end;
thick_line_ls_step_not_steep({X,_},Y,_Dx,_Dy,_Ystep,_E,X0,LSs) ->
    [{Y-1,X0,X}|[{Y,X0,X}|LSs]].

% Iterating the Y-axis
thick_line_ls_step_steep({X,X1},Y,Dx,Dy,Ystep,E, X0, LSs) when X =< X1 ->
    case E >= 0 of
	true ->
	    thick_line_ls_step_steep({X + 1,X1},Y+Ystep,Dx,Dy,Ystep,E - Dx + Dy,X, [{X,Y,Y+1}|LSs]);
	false ->
	    thick_line_ls_step_steep({X + 1,X1},Y,Dx,Dy,Ystep,E + Dy,X0, [{X,Y,Y+1}|LSs])
    end;
thick_line_ls_step_steep({_X,_},_Y,_Dx,_Dy,_Ystep,_E,_X0,LSs) -> 
    LSs.

% Text

text_horizontal_ls(Point, Font, Chars) ->
    {_Fw,Fh} = egd_font:size(Font),
    text_intervals(Point, Fh, Font, Chars, []).
    
% This is stupid. The starting point is the top left (Ptl) but the font
% offsets is relative to the bottom right origin,
%  {Xtl,Ytl} -------------------------
%            |                       |
%            |    Glyph BoundingBox  |
%            |     --------          |
%            |     |Bitmap| Gh       |
%       FH   |-Gx0-|Data  |          |
%            |     --------          |
%            |        |              |
%            |       Gy0             |
%            |        |              |
% Glyph (0,0)------------------------- Gxm (Glyph X move)
%                     FW
% Therefore, we need Yo, which is Yo = FH - Gy0 - Gh,
% Font height minus Glyph Y offset minus Glyph bitmap data boundingbox
% height.

text_intervals( _, _, _, [], Out) -> lists:flatten(Out);
text_intervals({Xtl,Ytl}, Fh, Font, [Code|Chars], Out) ->
    case catch begin {{_Gw, Gh, Gx0, Gy0, Gxm}, LSs} = egd_font:glyph(Font, Code),
    % Set offset points from translation matrix to point in TeInVe.
    Yo = Fh - Gh + Gy0,
    GLSs = text_intervals_vertical({Xtl+Gx0,Ytl+Yo},LSs, []),
    text_intervals({Xtl+Gxm,Ytl}, Fh, Font, Chars, [GLSs|Out])
	end of 
		{'EXIT', _} -> 
			text_intervals({Xtl,Ytl}, Fh, Font, Chars, Out);
		I -> 
			I
	end.

text_intervals_vertical( _, [], Out) -> Out;
text_intervals_vertical({Xtl, Ytl}, [LS|LSs], Out) -> 
    H = lists:foldl( 
	fun ({Xl,Xr}, RLSs) ->
	    [{Ytl, Xl + Xtl, Xr + Xtl}|RLSs]
	end, [], LS),
    text_intervals_vertical({Xtl, Ytl+1}, LSs, [H|Out]).


%%% E. PostScript implementation

eps(#image{ objects = Os, width = W, height = H}) ->
    list_to_binary([eps_header(W,H),eps_objects(H,Os),eps_footer()]).

eps_objects(H,Os) -> eps_objects(H,Os, []).
eps_objects(_,[], Out) -> lists:flatten(Out);
eps_objects(H,[O|Os], Out) -> eps_objects(H,Os, [eps_object(H,O)|Out]).

eps_object(H,#image_object{ type = text_horizontal, internals = {_Font,Text}, points = [{X,Y}], color={R,G,B,_}}) ->
    s("/Times-Roman findfont\n14 scalefont\nsetfont\n~.4f ~.4f ~.4f setrgbcolor\nnewpath\n~p ~p moveto\n(~s) show~n",
	[R,G,B,X,H-(Y + 10), Text]);
eps_object(H,#image_object{ type = filled_ellipse, points = [{X1,Y1p},{X2,Y2p}], color={R,G,B,_}}) ->
    Y1 = H - Y1p,
    Y2 = H - Y2p,
    Xr = trunc((X2-X1)/2),
    Yr = trunc((Y2-Y1)/2),
    Cx = X1 + Xr,
    Cy = Y1 + Yr,
    s("~.4f ~.4f ~.4f setrgbcolor\nnewpath\n~p ~p ~p ~p 0 360 ellipse fill\n", 
	[R,G,B,Cx,Cy,Xr,Yr]);
eps_object(H,#image_object{ type = arc, points = [P0, P1], internals = D, color={R,G,B,_}}) ->
    Es = egd_primitives:arc_to_edges(P0, P1, D),
    [s("~.4f ~.4f ~.4f setrgbcolor\n", [R,G,B])|lists:foldl(fun
    	({{X1,Y1},{X2,Y2}}, Eps) ->
    	    [s("newpath\n~p ~p moveto\n~p ~p lineto\n1 setlinewidth\nstroke\n", [X1,H-Y1,X2,H-Y2])|Eps]
    end, [], Es)];
 
eps_object(H,#image_object{ type = line, points = [{X1,Y1}, {X2,Y2}], color={R,G,B,_}}) ->
    s("~.4f ~.4f ~.4f setrgbcolor\nnewpath\n~p ~p moveto\n~p ~p lineto\n1 setlinewidth\nstroke\n", 
	[R,G,B,X1,H-Y1,X2,H-Y2]);
eps_object(H,#image_object{ type = rectangle, points = [{X1,Y1}, {X2,Y2}], color={R,G,B,_}}) ->
    s("~.4f ~.4f ~.4f setrgbcolor\nnewpath\n~p ~p moveto\n~p ~p lineto\n~p ~p lineto\n~p ~p lineto\n~p ~p lineto\n1 setlinewidth\nstroke\n", 
	[R,G,B,X1,H-Y1,X2,H-Y1,X2,H-Y2,X1,H-Y2,X1,H-Y1]);
eps_object(H,#image_object{ type = filled_rectangle, points = [{X1,Y1}, {X2,Y2}], color={R,G,B,_}}) ->
    s("~.4f ~.4f ~.4f setrgbcolor\nnewpath\n~p ~p moveto\n~p ~p lineto\n~p ~p lineto\n~p ~p lineto\n~p ~p lineto\nclosepath\nfill\n", 
	[R,G,B,X1,H-Y1,X2,H-Y1,X2,H-Y2,X1,H-Y2,X1,H-Y1]);
eps_object(_,_) -> "".

s(Format, Terms) -> lists:flatten(io_lib:format(Format, Terms)).

eps_header(W,H) ->
    s("%!PS-Adobe-3.0 EPSF-3.0\n%%Creator: Created by egd\n%%BoundingBox: 0 0 ~p ~p\n%%LanguageLevel: 2\n%%Pages: 1\n%%DocumentData: Clean7Bit\n",[W,H]) ++ 
    "%%BeginProlog\n/ellipse {7 dict begin\n/endangle exch def\n/startangle exch def\n/yradius exch def\n/xradius exch def\n/yC exch def\n/xC exch def\n"
    "/savematrix matrix currentmatrix def\nxC yC translate\nxradius yradius scale\n0 0 1 startangle endangle arc\nsavematrix setmatrix\nend\n} def\n"
    "%%EndProlog\n".

eps_footer() -> 
    "%%EOF\n".
