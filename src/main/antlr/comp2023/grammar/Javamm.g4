grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;
COMMENTINLINE : '//' ~[\r\n]* -> skip ;
COMMENTMULTILINE : '/*' .*? '*/' -> skip ;
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