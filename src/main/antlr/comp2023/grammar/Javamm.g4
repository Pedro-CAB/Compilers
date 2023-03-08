grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : [0-9]+ ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;
COMMENTINLINE : '//' ~[\r\n]* -> skip ;
COMMENTMULTILINE : '/*' .*? '*/' -> skip ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (statement)+ EOF
    ;

statement
    : 'if' '(' expression ')' statement ('else' 'if' '(' expression ')' statement)* ('else' statement)? #IfElse
    | 'do' statement 'while' '(' expression ')' ';' #DoWhile
    | 'while' '(' expression ')' statement #While
    | 'switch' '(' expression ')' '{' ('case' expression ':' statement* ('break' ';')?)* 'default' ':' statement* ('break' ';')? '}' #Switch
    | '{' statement* '}' #NestedStatements
    | var=ID '=' expression ';' #Assignment
    | expression ';' #ExprStmt
    ;

expression
    : '(' expression ')' #Parentheses
    | expression op=('++' | '--') #UnaryPostOp
    | op=('++' | '--' | '+' | '-' | '!' | '~') expression #UnaryPreOp
    | expression op=('*' | '/' | '%') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('<<' | '>>' | '>>>') expression #BinaryOp
    | expression op=('<' | '<=' | '>' | '>=' | 'instanceof') expression #BinaryOp
    | expression op=('==' | '!=' ) expression #BinaryOp
    | expression op='&' expression #BinaryOp
    | expression op='^' expression #BinaryOp
    | expression op='|' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | expression op='||' expression #BinaryOp
    | expression op='?:' expression #BinaryOp
    | expression op=('+=' | '-=' | '*=' | '/=' | '%=') expression #BinaryOp
    | value=INT #Integer
    | value=ID #Identifier
    | value=('true' | 'false') #Boolean
    | value='this' #Self
    ;