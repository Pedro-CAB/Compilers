grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [1-9][0-9]* ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;
COMMENTINLINE : '//' ~( '\r' | '\n')* -> skip;
COMMENTMULTILINE:'/*' (.)*? '*/' -> skip ;
WS : [ \t\n\r\f]+ -> skip ;

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
    : 'import ' value=ID ';' #ImportPackage
    ;

code
    : (statement)+
    ;

classDeclaration
    : classIdentification (classExtends)? (classImplements)?
    ;

classIdentification
    : ('public'|'private')? 'class' value=ID #CreatedClass
    ;

classExtends
    :'extends' value=ID #ExtendedClass
    ;

classImplements
    :'implements' value=ID #ImplementedClass
    ;

statement
    : value=ID '=' expression ';'   #Assignement
    ;

expression
    : op='!' expression #UnaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | value=INTEGER #Integer
    | value=ID #Identifier
    ;