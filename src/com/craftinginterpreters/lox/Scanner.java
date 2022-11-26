package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0; // first character in lexeme being scanned
    private int current = 0; // character being considered
    private int line = 1; // source line current is on (for location)
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    public Scanner(String source){
        this.source = source;
    }

    public List<Token> scanTokens(){
        while (!isAtEnd()){
            start = current;
            scanToken();
        }

        // appends file EOF token
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken(){
        char c = advance();
        switch (c) {
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case ',' -> addToken(COMMA);
            case '.' -> addToken(DOT);
            case '-' -> addToken(MINUS);
            case '+' -> addToken(PLUS);
            case ';' -> addToken(SEMI_COLON);
            case '*' -> addToken(STAR);
            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
            case '/' -> {
                if (match('/')) {
                    // using peek instead of match to lookahead for newline
                    // so we don't consume it and it can be used to increment the line count
                    while (peek() != '\n' && !isAtEnd()) advance();

                // multiline functionality
                }else if (match('*')){
                    char peekChar = peek();
                    char peekNextChar = peekNext();

                    // while the end symbols has not matched and it's not the end of the file
                    while ((peekChar != '*' || peekNextChar != '/') && !isAtEnd()){
                        if (peekChar == '/' && peekNextChar == '*') { // recursive call for nested multiline comments
                            scanToken();
                        } else {
                            advance(); // \n will be scanned by itself
                        }
                    }

                    if (isAtEnd()){
                        Lox.error(line, "Unterminated multi-line comment");
                    } else {
                        advance();
                        advance(); // consume "*/"
                    }
                } else{
                    addToken(SLASH);
                }
            }
            case ' ', '\r', '\t' -> {}
            case '\n' -> line++;
            case '"' -> string();
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                }else {
                    Lox.error(line, "Unexpected character");
                }
            }
        }
    }

    private boolean isAtEnd(){
        return current >= source.length();
    }

    private char advance(){ // for input
        return source.charAt(current++); // increments to the next character
    }

    private void addToken(TokenType type){ // for output
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal){
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean match(char expected){ // like conditional advance
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++; // move to next character
        return true;
    }

    private char peek(){
        // peek at the next character since current was incremented nu advance
        // does not consume the character
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()){
            if (peek() == '\n') line++; // multiline strings
            advance();
        }

        if (isAtEnd()){
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance(); // consumes closing "

        String value = source.substring(start+1, current-1); // trim quotes
        addToken(STRING, value);
    }

    private boolean isDigit(char c){
        return c >= '0' && c <= '9';
    }

    private void number(){
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())){ // decimal point followed by at least one digit
            advance(); // consume .

            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private char peekNext(){
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1); // peek at the second character
    }

    private boolean isAlpha(char c){
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c){
        return isAlpha(c) || isDigit(c);
    }

    private void identifier(){
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        if (type == null) type = IDENTIFIER;
        addToken(type);
    }


}
