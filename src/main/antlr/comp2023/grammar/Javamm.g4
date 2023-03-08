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

header //Header é um conjunto de Imports de Packages
    : packageImport*
    ;

packageImport //Exemplo: 'import JavaRandomPackage' (falta implementar '.')
    : 'import ' value=ID ';' #ImportPackage
    ;

fullClass //Uma Classe tem a estrutura declaração{código}
    : classDeclaration '{' classCode '}'
    ;

classDeclaration //Declaração da Classe Dividida em Etapas
    : classIdentification (classExtends)? (classImplements)?
    ;

classIdentification //Definição de Acessos e nome da classe
    : ('public'|'private')? 'class' value=ID #CreatedClass
    ;

classExtends //Classe Extendida pela Classe Criada (só pode extender no máximo uma)
    :'extends' value=ID #ExtendedClass
    ;

classImplements //Classes Implementadas pela Classe Criada (podem ser várias ou nenhuma)
    :'implements' value=ID #ImplementedClass
    | classImplements classImplements #ImplementedClasses
    ;

classCode //Conteúdo da Classe
    : (classField | classMethod)+
    ;

classField //Exemplo: 'int i;' ou 'int i = 1;'
    : value=ID (varAssignement|varDeclaration) ';' #FieldType
    ;

varAssignement //Associar um valor a uma variável
    : varDeclaration '=' expression
    | value=ID '=' expression
    ;

varDeclaration //Declarar uma variável
    : value=ID #VarName
    ;

classMethod //Exemplo: 'public int sum(int x, int y)'
    : ('public' | 'private') value=ID methodDefinition '{' methodCode* '}' #MethodReturnType
    ;

methodDefinition
    : value=ID '(' (methodArgument)? ')' #MethodName
    ;

methodArgument
    :value=ID varDeclaration #ArgumentType
    |methodArgument ',' methodArgument #Arguments
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

methodCode
    : value=ID (varAssignement|varDeclaration) ';' #VarType
    | varAssignement ';' #Assignement
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