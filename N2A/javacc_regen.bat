
@echo off

Rem IMPORTANT: Assumes the working directory is the project root.
Rem Example: cd C:\Users\dtrumbo\Projects\workspaces\eclipse-3.7-primary\NeuronsToAlgorithms\N2A

SET PROJ_ROOT=%~dp0

Rem Generate
cd "%PROJ_ROOT%src\gov\sandia\n2a\language\parse"
java -classpath "%PROJ_ROOT%lib\javacc-6.0\javacc.jar" jjtree grammar.jjt
java -classpath "%PROJ_ROOT%lib\javacc-6.0\javacc.jar" javacc grammar.jj

cd "%PROJ_ROOT%"
