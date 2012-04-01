-record(erlv8_fun_invocation, {
		  is_construct_call = false,
		  holder,
		  this,
		  ref,
		  vm,
		  ctx
		 }).
		  
-define(V8Obj(X),erlv8_object:new(X)).
-define(V8Arr(X),erlv8_array:new(X)).
-define(V8TERM_TO_ATOM(X),list_to_atom(binary_to_list(X))).


-record(erlv8_object, { resource, vm }).
-record(erlv8_fun, { resource, vm }).
-record(erlv8_array, { resource, vm }).

		  
