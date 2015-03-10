/* Generated By:JJTree&JavaCC: Do not edit this line. ExpressionParserTokenManager.java */
/*
Copyright 2013 Sandia Corporation.
Under the terms of Contract DE-AC04-94AL85000 with Sandia Corporation,
the U.S. Government retains certain rights in this software.
Distributed under the BSD-3 license. See the file LICENSE for details.
*/

package gov.sandia.n2a.language.parse;
import java.lang.reflect.Field;
import java.io.StringReader;

import gov.sandia.n2a.language.Function;
import gov.sandia.n2a.language.FunctionList;
import replete.util.ReflectionUtil;

import javax.measure.unit.*;

/** Token Manager. */
public class ExpressionParserTokenManager implements ExpressionParserConstants
{

  /** Debug output. */
  public  java.io.PrintStream debugStream = System.out;
  /** Set debug output. */
  public  void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }
private final int jjStopStringLiteralDfa_0(int pos, long active0)
{
   switch (pos)
   {
      case 0:
         if ((active0 & 0x10000000L) != 0L)
            return 14;
         if ((active0 & 0x400000L) != 0L)
         {
            jjmatchedKind = 14;
            return -1;
         }
         if ((active0 & 0x80000000L) != 0L)
            return 29;
         return -1;
      default :
         return -1;
   }
}
private final int jjStartNfa_0(int pos, long active0)
{
   return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0), pos + 1);
}
private int jjStopAtPos(int pos, int kind)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   return pos + 1;
}
private int jjMoveStringLiteralDfa0_0()
{
   switch(curChar)
   {
      case 33:
         jjmatchedKind = 33;
         return jjMoveStringLiteralDfa1_0(0x2000000L);
      case 37:
         return jjStopAtPos(0, 32);
      case 38:
         return jjMoveStringLiteralDfa1_0(0x4000000L);
      case 40:
         return jjStopAtPos(0, 37);
      case 41:
         return jjStopAtPos(0, 38);
      case 42:
         return jjStopAtPos(0, 30);
      case 43:
         return jjStartNfaWithStates_0(0, 28, 14);
      case 44:
         return jjStopAtPos(0, 18);
      case 45:
         return jjStopAtPos(0, 29);
      case 47:
         return jjStartNfaWithStates_0(0, 31, 29);
      case 59:
         return jjStopAtPos(0, 19);
      case 60:
         jjmatchedKind = 21;
         return jjMoveStringLiteralDfa1_0(0x800000L);
      case 61:
         return jjMoveStringLiteralDfa1_0(0x400000L);
      case 62:
         jjmatchedKind = 20;
         return jjMoveStringLiteralDfa1_0(0x1000000L);
      case 91:
         return jjStopAtPos(0, 35);
      case 93:
         return jjStopAtPos(0, 36);
      case 94:
         return jjStopAtPos(0, 34);
      case 123:
         return jjStopAtPos(0, 39);
      case 124:
         return jjMoveStringLiteralDfa1_0(0x8000000L);
      default :
         return jjMoveNfa_0(0, 0);
   }
}
private int jjMoveStringLiteralDfa1_0(long active0)
{
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(0, active0);
      return 1;
   }
   switch(curChar)
   {
      case 38:
         if ((active0 & 0x4000000L) != 0L)
            return jjStopAtPos(1, 26);
         break;
      case 61:
         if ((active0 & 0x400000L) != 0L)
            return jjStopAtPos(1, 22);
         else if ((active0 & 0x800000L) != 0L)
            return jjStopAtPos(1, 23);
         else if ((active0 & 0x1000000L) != 0L)
            return jjStopAtPos(1, 24);
         else if ((active0 & 0x2000000L) != 0L)
            return jjStopAtPos(1, 25);
         break;
      case 124:
         if ((active0 & 0x8000000L) != 0L)
            return jjStopAtPos(1, 27);
         break;
      default :
         break;
   }
   return jjStartNfa_0(0, active0);
}
private int jjStartNfaWithStates_0(int pos, int kind, int state)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) { return pos + 1; }
   return jjMoveNfa_0(state, pos + 1);
}
static final long[] jjbitVec0 = {
   0xfffffffffffffffeL, 0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffffffffffffL
};
static final long[] jjbitVec2 = {
   0x0L, 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL
};
private int jjMoveNfa_0(int startState, int curPos)
{
   int startsAt = 0;
   jjnewStateCnt = 40;
   int i = 1;
   jjstateSet[0] = startState;
   int kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         do
         {
            switch(jjstateSet[--i])
            {
               case 29:
                  if (curChar == 42)
                     jjCheckNAddTwoStates(35, 36);
                  else if (curChar == 47)
                     jjCheckNAddStates(0, 2);
                  break;
               case 0:
                  if ((0x3ff000000000000L & l) != 0L)
                  {
                     if (kind > 7)
                        kind = 7;
                     jjCheckNAddStates(3, 6);
                  }
                  else if ((0x401000000000L & l) != 0L)
                  {
                     if (kind > 10)
                        kind = 10;
                     jjCheckNAddTwoStates(11, 12);
                  }
                  else if (curChar == 47)
                     jjAddStates(7, 8);
                  else if (curChar == 58)
                     jjCheckNAdd(14);
                  else if (curChar == 43)
                     jjCheckNAdd(14);
                  else if (curChar == 61)
                  {
                     if (kind > 14)
                        kind = 14;
                  }
                  else if (curChar == 34)
                     jjCheckNAddStates(9, 11);
                  if (curChar == 46)
                     jjCheckNAdd(1);
                  break;
               case 1:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 7)
                     kind = 7;
                  jjCheckNAddTwoStates(1, 2);
                  break;
               case 3:
                  if ((0x280000000000L & l) != 0L)
                     jjCheckNAdd(4);
                  break;
               case 4:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 7)
                     kind = 7;
                  jjCheckNAdd(4);
                  break;
               case 5:
                  if (curChar == 34)
                     jjCheckNAddStates(9, 11);
                  break;
               case 6:
                  if ((0xfffffffbffffdbffL & l) != 0L)
                     jjCheckNAddStates(9, 11);
                  break;
               case 8:
                  if ((0x8400000000L & l) != 0L)
                     jjCheckNAddStates(9, 11);
                  break;
               case 9:
                  if (curChar == 34 && kind > 9)
                     kind = 9;
                  break;
               case 10:
                  if ((0x401000000000L & l) == 0L)
                     break;
                  if (kind > 10)
                     kind = 10;
                  jjCheckNAddTwoStates(11, 12);
                  break;
               case 11:
                  if ((0x3ff401000000000L & l) == 0L)
                     break;
                  if (kind > 10)
                     kind = 10;
                  jjCheckNAddTwoStates(11, 12);
                  break;
               case 12:
                  if (curChar != 39)
                     break;
                  if (kind > 10)
                     kind = 10;
                  jjCheckNAdd(12);
                  break;
               case 13:
               case 14:
                  if (curChar == 61 && kind > 14)
                     kind = 14;
                  break;
               case 15:
                  if (curChar == 43)
                     jjCheckNAdd(14);
                  break;
               case 16:
                  if (curChar == 58)
                     jjCheckNAdd(14);
                  break;
               case 17:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 7)
                     kind = 7;
                  jjCheckNAddStates(3, 6);
                  break;
               case 18:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjCheckNAddTwoStates(18, 19);
                  break;
               case 19:
                  if (curChar != 46)
                     break;
                  if (kind > 7)
                     kind = 7;
                  jjCheckNAddTwoStates(20, 21);
                  break;
               case 20:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 7)
                     kind = 7;
                  jjCheckNAddTwoStates(20, 21);
                  break;
               case 22:
                  if ((0x280000000000L & l) != 0L)
                     jjCheckNAdd(23);
                  break;
               case 23:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 7)
                     kind = 7;
                  jjCheckNAdd(23);
                  break;
               case 24:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 7)
                     kind = 7;
                  jjCheckNAddTwoStates(24, 25);
                  break;
               case 26:
                  if ((0x280000000000L & l) != 0L)
                     jjCheckNAdd(27);
                  break;
               case 27:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 7)
                     kind = 7;
                  jjCheckNAdd(27);
                  break;
               case 28:
                  if (curChar == 47)
                     jjAddStates(7, 8);
                  break;
               case 30:
                  if ((0xffffffffffffdbffL & l) != 0L)
                     jjCheckNAddStates(0, 2);
                  break;
               case 31:
                  if ((0x2400L & l) != 0L && kind > 5)
                     kind = 5;
                  break;
               case 32:
                  if (curChar == 10 && kind > 5)
                     kind = 5;
                  break;
               case 33:
                  if (curChar == 13)
                     jjstateSet[jjnewStateCnt++] = 32;
                  break;
               case 34:
                  if (curChar == 42)
                     jjCheckNAddTwoStates(35, 36);
                  break;
               case 35:
                  if ((0xfffffbffffffffffL & l) != 0L)
                     jjCheckNAddTwoStates(35, 36);
                  break;
               case 36:
                  if (curChar == 42)
                     jjAddStates(12, 13);
                  break;
               case 37:
                  if ((0xffff7fffffffffffL & l) != 0L)
                     jjCheckNAddTwoStates(38, 36);
                  break;
               case 38:
                  if ((0xfffffbffffffffffL & l) != 0L)
                     jjCheckNAddTwoStates(38, 36);
                  break;
               case 39:
                  if (curChar == 47 && kind > 6)
                     kind = 6;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 0:
               case 11:
                  if ((0x7fffffe87fffffeL & l) == 0L)
                     break;
                  if (kind > 10)
                     kind = 10;
                  jjCheckNAddTwoStates(11, 12);
                  break;
               case 2:
                  if ((0x2000000020L & l) != 0L)
                     jjAddStates(14, 15);
                  break;
               case 6:
                  if ((0xffffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(9, 11);
                  break;
               case 7:
                  if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 8;
                  break;
               case 8:
                  if ((0x14404410000000L & l) != 0L)
                     jjCheckNAddStates(9, 11);
                  break;
               case 21:
                  if ((0x2000000020L & l) != 0L)
                     jjAddStates(16, 17);
                  break;
               case 25:
                  if ((0x2000000020L & l) != 0L)
                     jjAddStates(18, 19);
                  break;
               case 30:
                  jjAddStates(0, 2);
                  break;
               case 35:
                  jjCheckNAddTwoStates(35, 36);
                  break;
               case 37:
               case 38:
                  jjCheckNAddTwoStates(38, 36);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int hiByte = (int)(curChar >> 8);
         int i1 = hiByte >> 6;
         long l1 = 1L << (hiByte & 077);
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 6:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjAddStates(9, 11);
                  break;
               case 30:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjAddStates(0, 2);
                  break;
               case 35:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjCheckNAddTwoStates(35, 36);
                  break;
               case 37:
               case 38:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjCheckNAddTwoStates(38, 36);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 40 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
private final int jjStopStringLiteralDfa_1(int pos, long active0)
{
   switch (pos)
   {
      default :
         return -1;
   }
}
private final int jjStartNfa_1(int pos, long active0)
{
   return jjMoveNfa_1(jjStopStringLiteralDfa_1(pos, active0), pos + 1);
}
private int jjMoveStringLiteralDfa0_1()
{
   switch(curChar)
   {
      case 125:
         return jjStopAtPos(0, 40);
      default :
         return jjMoveNfa_1(0, 0);
   }
}
private int jjMoveNfa_1(int startState, int curPos)
{
   int startsAt = 0;
   jjnewStateCnt = 1;
   int i = 1;
   jjstateSet[0] = startState;
   int kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((0x3ffef0000000000L & l) == 0L)
                     break;
                  kind = 41;
                  jjstateSet[jjnewStateCnt++] = 0;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 0:
                  if ((0x7fffffec7fffffeL & l) == 0L)
                     break;
                  kind = 41;
                  jjstateSet[jjnewStateCnt++] = 0;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int hiByte = (int)(curChar >> 8);
         int i1 = hiByte >> 6;
         long l1 = 1L << (hiByte & 077);
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 1 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
static final int[] jjnextStates = {
   30, 31, 33, 18, 19, 24, 25, 29, 34, 6, 7, 9, 37, 39, 3, 4, 
   22, 23, 26, 27, 
};
private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2)
{
   switch(hiByte)
   {
      case 0:
         return ((jjbitVec2[i2] & l2) != 0L);
      default :
         if ((jjbitVec0[i1] & l1) != 0L)
            return true;
         return false;
   }
}

/** Token literal values. */
public static final String[] jjstrLiteralImages = {
"", null, null, null, null, null, null, null, null, null, null, null, null, 
null, null, null, null, null, "\54", "\73", "\76", "\74", "\75\75", "\74\75", 
"\76\75", "\41\75", "\46\46", "\174\174", "\53", "\55", "\52", "\57", "\45", "\41", 
"\136", "\133", "\135", "\50", "\51", "\173", "\175", null, };

/** Lexer state names. */
public static final String[] lexStateNames = {
   "DEFAULT",
   "IN_UNITS",
};

/** Lex State array. */
public static final int[] jjnewLexState = {
   -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
   -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, 0, -1, 
};
static final long[] jjtoToken = {
   0x3fffffc4681L, 
};
static final long[] jjtoSkip = {
   0x7eL, 
};
protected JavaCharStream input_stream;
private final int[] jjrounds = new int[40];
private final int[] jjstateSet = new int[80];
protected char curChar;
/** Constructor. */
public ExpressionParserTokenManager(JavaCharStream stream){
   if (JavaCharStream.staticFlag)
      throw new Error("ERROR: Cannot use a static CharStream class with a non-static lexical analyzer.");
   input_stream = stream;
}

/** Constructor. */
public ExpressionParserTokenManager(JavaCharStream stream, int lexState){
   this(stream);
   SwitchTo(lexState);
}

/** Reinitialise parser. */
public void ReInit(JavaCharStream stream)
{
   jjmatchedPos = jjnewStateCnt = 0;
   curLexState = defaultLexState;
   input_stream = stream;
   ReInitRounds();
}
private void ReInitRounds()
{
   int i;
   jjround = 0x80000001;
   for (i = 40; i-- > 0;)
      jjrounds[i] = 0x80000000;
}

/** Reinitialise parser. */
public void ReInit(JavaCharStream stream, int lexState)
{
   ReInit(stream);
   SwitchTo(lexState);
}

/** Switch to specified lex state. */
public void SwitchTo(int lexState)
{
   if (lexState >= 2 || lexState < 0)
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
   else
      curLexState = lexState;
}

protected Token jjFillToken()
{
   final Token t;
   final String curTokenImage;
   final int beginLine;
   final int endLine;
   final int beginColumn;
   final int endColumn;
   String im = jjstrLiteralImages[jjmatchedKind];
   curTokenImage = (im == null) ? input_stream.GetImage() : im;
   beginLine = input_stream.getBeginLine();
   beginColumn = input_stream.getBeginColumn();
   endLine = input_stream.getEndLine();
   endColumn = input_stream.getEndColumn();
   t = Token.newToken(jjmatchedKind, curTokenImage);

   t.beginLine = beginLine;
   t.endLine = endLine;
   t.beginColumn = beginColumn;
   t.endColumn = endColumn;

   return t;
}

int curLexState = 0;
int defaultLexState = 0;
int jjnewStateCnt;
int jjround;
int jjmatchedPos;
int jjmatchedKind;

/** Get the next Token. */
public Token getNextToken() 
{
  Token matchedToken;
  int curPos = 0;

  EOFLoop :
  for (;;)
  {
   try
   {
      curChar = input_stream.BeginToken();
   }
   catch(java.io.IOException e)
   {
      jjmatchedKind = 0;
      matchedToken = jjFillToken();
      return matchedToken;
   }

   switch(curLexState)
   {
     case 0:
       try { input_stream.backup(0);
          while (curChar <= 32 && (0x100002600L & (1L << curChar)) != 0L)
             curChar = input_stream.BeginToken();
       }
       catch (java.io.IOException e1) { continue EOFLoop; }
       jjmatchedKind = 0x7fffffff;
       jjmatchedPos = 0;
       curPos = jjMoveStringLiteralDfa0_0();
       break;
     case 1:
       jjmatchedKind = 0x7fffffff;
       jjmatchedPos = 0;
       curPos = jjMoveStringLiteralDfa0_1();
       break;
   }
     if (jjmatchedKind != 0x7fffffff)
     {
        if (jjmatchedPos + 1 < curPos)
           input_stream.backup(curPos - jjmatchedPos - 1);
        if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L)
        {
           matchedToken = jjFillToken();
       if (jjnewLexState[jjmatchedKind] != -1)
         curLexState = jjnewLexState[jjmatchedKind];
           return matchedToken;
        }
        else
        {
         if (jjnewLexState[jjmatchedKind] != -1)
           curLexState = jjnewLexState[jjmatchedKind];
           continue EOFLoop;
        }
     }
     int error_line = input_stream.getEndLine();
     int error_column = input_stream.getEndColumn();
     String error_after = null;
     boolean EOFSeen = false;
     try { input_stream.readChar(); input_stream.backup(1); }
     catch (java.io.IOException e1) {
        EOFSeen = true;
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
        if (curChar == '\n' || curChar == '\r') {
           error_line++;
           error_column = 0;
        }
        else
           error_column++;
     }
     if (!EOFSeen) {
        input_stream.backup(1);
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
     }
     throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
  }
}

private void jjCheckNAdd(int state)
{
   if (jjrounds[state] != jjround)
   {
      jjstateSet[jjnewStateCnt++] = state;
      jjrounds[state] = jjround;
   }
}
private void jjAddStates(int start, int end)
{
   do {
      jjstateSet[jjnewStateCnt++] = jjnextStates[start];
   } while (start++ != end);
}
private void jjCheckNAddTwoStates(int state1, int state2)
{
   jjCheckNAdd(state1);
   jjCheckNAdd(state2);
}

private void jjCheckNAddStates(int start, int end)
{
   do {
      jjCheckNAdd(jjnextStates[start]);
   } while (start++ != end);
}

}
