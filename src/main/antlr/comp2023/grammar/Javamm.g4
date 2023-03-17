grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : ([0] | [1-9][0-9]*) ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;
COMMENTINLINE : '//' ~[\r\n]* -> skip ;
COMMENTMULTILINE : '/*' .*? '*/' -> skip ;
WS : [ \t\n\r\f]+ -> skip ;

program
    : packageImport* classDeclaration '{' classCode '}' statement*
    ;

packageImport //Exemplo: 'import JavaRandomPackage' (falta implementar '.')
    : 'import ' (path=ID '.')* value=ID ';' #ImportPackage
    ;

classDeclaration //Declaração da Classe Dividida em Etapas
    : classIdentification (classExtends)? (classImplements)?
    ;

classIdentification //Definição de Acessos e nome da classe
    : ('public'|'private')? 'class' value=ID #ClassName
    ;

classExtends //Classe Extendida pela Classe Criada (só pode extender no máximo uma)
    :'extends' value=ID #SuperclassName
    ;

classImplements //Classes Implementadas pela Classe Criada (podem ser várias ou nenhuma)
    :'implements' value=ID #ImplementedClass
    | classImplements classImplements #ImplementedClasses
    ;

classCode //Conteúdo da Classe
    : (statement | classMethod)*
    ;

classMethod //Exemplo: 'public int sum(int x, int y)'
    : ('public' | 'private' | 'static' )? type=ID name=ID '(' (methodArgument ','?)* ')' '{' statement* '}' #MethodDeclaration
    ;

methodArgument
    : type=ID '[]'? var=ID #Argument
    ;

statement
    : 'if' '(' expression ')' '{' statement '}' ('else' 'if' '(' expression ')' '{' statement '}')* ('else' '{'statement '}')? #IfElse
    | 'do' '{' statement '}' 'while' '(' expression ')' ';' #DoWhile
    | 'while' '(' expression ')' '{'statement'}' #While
    | 'switch' '(' expression ')' '{' ('case' expression ':' statement* ('break' ';')?)* 'default' ':' statement* ('break' ';')? '}' #Switch
    | '{' statement* '}' #NestedStatements
    | type=ID '[]' var=ID ('=' expression)? ';' #ArrayDeclaration
    | type=ID var=ID ('=' expression)? ';' #Declaration
    | var=ID '=' expression ';' #Assignment
    | var=ID '[' expression ']' '=' expression ';' #ArrayAssignment
    | 'return' expression ';' #Return
    | expression ';' #ExprStmt
    ;

expression
    : '(' expression ')' #Scope
    | expression '.length'+ #Length
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
    | 'new' type=ID '()'? ('[' expression? ']')* #NewObject
    | className=ID methodCall*  #MethodCalls
    | value=INT #Integer
    | value=ID #Identifier
    | value=('true' | 'false') #Boolean
    | expression ('[' expression ']')+ #ArrayAcess
    | value='this' #Self
    ;

methodCall
    : '.' methodName=ID ('()' | '(' (expression ','?)* ')' )
    ;