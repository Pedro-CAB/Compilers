grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;
COMMENTINLINE : '//' ~( '\r' | '\n')* -> skip;
COMMENTMULTILINE:'/*' (.)*? '*/' -> skip ;
WS : [ \t\n\r\f]+ -> skip ;
PACKAGENAME : [a-zA-Z_]+;
CLASSNAME : [A-Z][a-zA-Z_0-9]* ;

program
    : header fullClass
    ;

header
    : classImport*
    ;

fullClass
    : classDeclaration '{' code '}'
    ;

classImport
    : 'import ' PACKAGENAME ';' #ImportPackage
    ;

code
    : (statement)+
    ;

classDeclaration
    : ('public'|'private')? 'class' CLASSNAME ('extends' CLASSNAME)? ('implements' CLASSNAME)? #DeclaredClass
    ;

statement
    : expression ';'
    | ID '=' INTEGER ';'
    ;

expression
    : op='!' expression #UnaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | value=INTEGER #Integer
    | value=ID #Identifier
    ;