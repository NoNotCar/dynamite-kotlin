package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import kotlin.math.pow
import kotlin.random.Random

class MyBot : Bot {
    val rpsMoves = listOf(Move.R, Move.P, Move.S)
    val r_cache =
        mutableMapOf<Pair<Int, Int>, List<Int>>() //so we don't make a new list every ranInt call

    override fun makeMove(gamestate: Gamestate): Move {
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move
        val tar_draw=target_draw_number(gamestate)
        if (dynamiteLeft(gamestate) > 0 && pointsThisRound(gamestate) >= ranInt(tar_draw-2, tar_draw)) {
            if (pointsThisRound(gamestate,Move.D)>=3){
                //opponent is just dynamiting on draw...
                return Move.W
            }else if (gotCountered(gamestate) && ranInt(1)==1){
                //they might play W, do RPS instead...
            }else {
                return Move.D
            }
        }
        return rpsMoves.shuffled().first()
    }

    fun dynamiteLeft(gamestate: Gamestate): Int {
        return 100 - gamestate.rounds.map { if (it.p1 == Move.D) 1 else 0 }.sum()
    }
    fun target_draw_number(gamestate: Gamestate):Int{
        //return how many draws we need for optimal dynamite conservation
        val dynamite_fraction=dynamiteLeft(gamestate)/2000.0
        if (dynamite_fraction==0.0){return 9999}
        else{
            var n=0
            while (0.33333.pow(n)>dynamite_fraction){
                n++
            }
            return n
        }

    }
    fun gotCountered(gamestate: Gamestate): Boolean {
        //the opponent countered our dynamite play!
        return gamestate.rounds.any { it.p1 == Move.D && it.p2 == Move.W }
    }

    fun pointsThisRound(gamestate: Gamestate,onlyCount:Move?=null): Int {
        var points = 0
        for (round in gamestate.rounds.asReversed()) {
            if (round.p1 == round.p2 && (onlyCount==null || round.p1==onlyCount)) {
                points++
            } else {
                break
            }
        }
        return points
    }

    fun ranInt(to: Int): Int {
        return ranInt(0, to)
    }

    fun ranInt(from: Int, to: Int): Int {
        //because it be broken
        if (r_cache.containsKey(Pair(from, to))) {
            return r_cache[Pair(from, to)]?.shuffled()?.first() ?: from
        }
        r_cache[Pair(from, to)] = (from..to).toList()
        return ranInt(from, to)
    }

    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        //println("Started new match")
    }
}