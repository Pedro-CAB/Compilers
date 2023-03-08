grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;
COMMENTINLINE : '//' ~[\r\n]* -> skip ;
COMMENTMULTILINE : '/*' .*? '*/' -> skip ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (statement)+ EOF
    ;

statement
    : expression ';' #ExprStmt
    | var=ID '=' expression ';' #Assignment
    ;

expression
    : '(' expression ')' #Parentheses
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | value=INTEGER #Integer
    | value=ID #Identifier
    ;
