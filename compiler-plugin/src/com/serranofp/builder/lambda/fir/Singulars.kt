package com.serranofp.builder.lambda.fir

// reworked from https://github.com/projectlombok/lombok/blob/master/src/core/lombok/core/handlers/Singulars.java
object Singulars {
    val SINGULAR_STORE: List<String> = buildList {
        for (line in SingularsFile.lines()) {
            val line = line.trim { it <= ' ' }
            when {
                line.startsWith("#") || line.isEmpty() -> { }
                line.endsWith(" =") -> {
                    add(line.substring(0, line.length - 2))
                    add("")
                }
                else -> {
                    val idx = line.indexOf(" = ")
                    add(line.substring(0, idx))
                    add(line.substring(idx + 3))
                }
            }
        }
    }

    fun from(plural: String): String? {
        val inLen = plural.length
        for (i in SINGULAR_STORE.indices step 2) {
            val lastPart = SINGULAR_STORE[i]
            val wholeWord = lastPart.first().isUpperCase()
            val endingOnly = if (lastPart.first() == '-') 1 else 0
            val len = lastPart.length
            if (inLen < len) continue
            if (!plural.regionMatches(inLen - len + endingOnly, lastPart, endingOnly, len - endingOnly, ignoreCase = true)) continue
            if (wholeWord && inLen != len && !plural[inLen - len].isUpperCase()) continue

            val replacement = SINGULAR_STORE[i + 1]
            if (replacement == "!") return null

            val capitalizeFirst = replacement.isNotEmpty() && plural[inLen - len + endingOnly].isUpperCase()
            val pre = plural.substring(0, inLen - len + endingOnly)
            val post = if (capitalizeFirst) (replacement.first().uppercaseChar() + replacement.substring(1)) else replacement
            return pre + post
        }

        return null
    }
}

private const val SingularsFile = """
#Based on https://github.com/rails/rails/blob/efff6c1fd4b9e2e4c9f705a45879373cb34a5b0e/activesupport/lib/active_support/inflections.rb

quizzes = quiz
matrices = matrix
indices = index
vertices = vertex
statuses = status
aliases = alias
alias = !
species = !
Axes = !
-axes = axe
sexes = sex
Testes = testis
movies = movie
octopodes = octopus
buses = bus
Mice = mouse
Lice = louse
News = !
# We could add more detail (axemen, boatsmen, boogymen, cavemen, gentlemen, etc, but (A) there's stuff like 'cerumen', and (B) the 'men' ending is common in singulars and other languages.)
# Therefore, the odds of a mistake are too high, so other than these 2 well known cases, force the explicit singular.
Men = man
Women = woman
minutiae = minutia
shoes = shoe
synopses = synopsis
prognoses = prognosis
theses = thesis
diagnoses = diagnosis
bases = base
analyses = analysis
Crises = crisis
children = child
moves = move
zombies = zombie
-quies = quy
-us = !
-is = !
series = !
-ies = y
-oes = o
hives = hive
-tives = tive
-sses = ss
-ches = ch
-xes = x
-shes = sh
-lves = lf
-rves = rf
saves = save
Leaves = leaf
-ves = !
-ss = !
-us = !
-s = 
"""