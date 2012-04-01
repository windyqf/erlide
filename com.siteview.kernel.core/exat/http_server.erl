%
% http_server.erl
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
-module (http_server).

-export ([decode/1,
          media_type_decode/1,
          terms_to_string/1,
          terms_to_atom/1,
          terms_to_tuple/1,
          terms_to_integer/1,
          trim/1,
          tokenize/1]).

-author ('csanto@diit.unict.it').

-define (CRLF, "\r\n").




tokenize ([]) -> [{'$end',1}];
tokenize ([H|T]) -> [ {list_to_atom ([H]),1} | tokenize (T) ].


terms_to_string ([]) -> [];
terms_to_string ([H|T]) ->
  [ L | _ ] = atom_to_list (element (1, H)),
  [ L | terms_to_string (T) ].

terms_to_atom (X) ->
  list_to_atom (terms_to_string (X)).

terms_to_tuple (X) ->
  list_to_tuple (terms_to_string (X)).

terms_to_integer (X) ->
  Z = terms_to_string (X),
  list_to_integer (Z).

ltrim ([]) ->
  [];
ltrim ([$ | T]) ->
  ltrim (T);
ltrim ([H | T]) ->
  [H | T].

trim (S) ->
  lists:reverse (ltrim (lists:reverse (ltrim (S)))).

decode (M) ->
  T = tokenize (M),
  http_parser:parse (T).


media_type_decode (M) ->
  T = tokenize (M),
  media_type:parse (T).





