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
    : packageImport* (classDeclaration ('{' classBody '}'))? statement*
    ;

packageImport
    : 'import ' path=packagePath value=ID ';' #ImportPackage
    ;

packagePath
    : (ID '.')*
    ;

classDeclaration //Declaração da Classe Dividida em Etapas
    : classIdentification (classExtends)? (classImplements)?
    ;

classIdentification //Definição de Acessos e nome da classe
    : modifier* 'class' value=ID #ClassName
    ;

classExtends //Classe Extendida pela Classe Criada (só pode extender no máximo uma)
    :'extends' value=ID #SuperclassName
    ;

classImplements //Classes Implementadas pela Classe Criada (podem ser várias ou nenhuma)
    :'implements' value=ID #ImplementedClass
    | classImplements classImplements #ImplementedClasses
    ;

classBody //Conteúdo da Classe
    : (classField | method)*
    ;

classField
    : statement
    ;

method //Exemplo: 'public int sum(int x, int y)'
    : modifier* varType name=ID ('()' | '(' (methodArgument ','?)* ')') methodBody #ClassMethod
    ;

methodBody
    : '{' statement* '}'
    ;

modifier
    : val='public'
    | val='private'
    | val='static'
    | val='final'
    ;

methodArgument
    : varType var=ID #Argument
    ;

varType
    : type=ID #Type
    | type=ID '[]' #ArrayType
    ;

statement
    : 'if' '(' expression ')' statement ('else' 'if' '(' expression ')' statement)* ('else' statement )? #IfElse
    | 'do' statement 'while' '(' expression ')' ';' #DoWhile
    | 'while' '(' expression ')' statement #While
    | 'for' '(' varType? var=ID ('=' expression)? ';' expression ';' expression')' statement #ForCycle
    | 'switch' '(' expression ')' '{' ('case' expression ':' statement* ('break' ';')?)* 'default' ':' statement* ('break' ';')? '}' #Switch
    | '{' statement* '}' #NestedStatements
    | varType var=ID ('=' expression)? ';' #Declaration
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
    | value=INT #Integer
    | value=ID #Identifier
    | value=('true' | 'false') #Boolean
    | expression ('[' expression ']')+ #ArrayAcess
    | value='this' #Self
    | className=ID methodCall+  #MethodCalls
    ;

methodCall
    : '.' methodName=ID ('()' | '(' methodArg* ')' )
    ;

methodArg
    : expression ','?
    ;