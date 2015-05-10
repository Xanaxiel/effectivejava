(ns app.symbol_solver
  (:use [app.javaparser])
  (:use [app.operations])
  (:use [app.itemsOnLifecycle])
  (:use [app.utils])
  (:require [instaparse.core :as insta])
  (:import [app.operations Operation]))

(import com.github.javaparser.JavaParser)
(import com.github.javaparser.ast.CompilationUnit)
(import com.github.javaparser.ast.Node)
(import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration)
(import com.github.javaparser.ast.body.EnumDeclaration)
(import com.github.javaparser.ast.body.EnumConstantDeclaration)
(import com.github.javaparser.ast.body.ConstructorDeclaration)
(import com.github.javaparser.ast.body.FieldDeclaration)
(import com.github.javaparser.ast.body.MethodDeclaration)
(import com.github.javaparser.ast.body.ModifierSet)
(import com.github.javaparser.ast.body.TypeDeclaration)
(import com.github.javaparser.ast.body.VariableDeclaratorId)
(import com.github.javaparser.ast.stmt.ExpressionStmt)
(import com.github.javaparser.ast.stmt.BlockStmt)
(import com.github.javaparser.ast.expr.MethodCallExpr)
(import com.github.javaparser.ast.expr.NameExpr)
(import com.github.javaparser.ast.expr.AssignExpr)
(import com.github.javaparser.ast.visitor.DumpVisitor)

(defprotocol scope
  ; for example in a BlockStmt containing statements [a b c d e], when solving symbols in the context of c
  ; it will contains only statements preceeding it [a b]
  (solveSymbol [this context nameToSolve]))

(extend-protocol scope
  NameExpr
  (solveSymbol [this context nameToSolve] (solveSymbol (.getParentNode this) this nameToSolve)))

(extend-protocol scope
  AssignExpr
  (solveSymbol [this context nameToSolve] (solveSymbol (.getParentNode this) this nameToSolve)))

(defn find-index [elmts elmt]
  (if (empty? elmts)
    -1
    (if (identical? (first elmts) elmt)
      0
      (let [rest (find-index (rest elmts) elmt)]
        (if (= -1 rest)
          -1
          (+ 1 rest))))))

(defn preceedingChildren [children child]
  (let [i (find-index children child)]
    (if (= -1 i)
      (throw (RuntimeException. "Not found"))
      (take i children))))

(extend-protocol scope
  BlockStmt
  (solveSymbol [this context nameToSolve]
    (let [preceedingSiblings (preceedingChildren (.getStmts this) context)
          solvedSymbols (map (fn [c] (solveSymbol c nil nameToSolve)) preceedingSiblings)
          solvedSymbols' (filter (fn [s] (not (nil? s))) solvedSymbols)]
      (first solvedSymbols'))))

(extend-protocol scope
  ExpressionStmt
  (solveSymbol [this context nameToSolve]
    (let [fromExpr (solveSymbol (.getExpression this) nil nameToSolve)]
          (or fromExpr (solveSymbol (.getParentNode this) this nameToSolve)))))

;(defn solveSymbol [nameExpr]
;  )

(defn solveNameExpr [nameExpr]
  ; TODO consider local variables
  ; TODO consider fields
  ; TODO consider inherited fields
  (let [name (.getName nameExpr)]
    (solveSymbol nameExpr nil name)))