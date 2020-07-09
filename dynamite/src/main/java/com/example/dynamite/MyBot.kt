package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.softwire.dynamite.game.Round
import java.util.*
import kotlin.math.pow
import kotlin.random.Random

class MyBot : Bot {
    val CONFIDENCE_THRESHOLD=1 //size of past moves before we're confident
    val P_THRESHOLD=0.5
    val rpsMoves = listOf(Move.R, Move.P, Move.S)
    val rpsCounters= mapOf(Pair(Move.R,Move.P), Pair(Move.P,Move.S), Pair(Move.S,Move.R))
    val allMoves= listOf(Move.R,Move.P,Move.S,Move.W,Move.D)
    val r_cache =
        mutableMapOf<Pair<Int, Int>, List<Int>>() //so we don't make a new list every ranInt call

    override fun makeMove(gamestate: Gamestate): Move {
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move
        val tar_draw=target_draw_number(gamestate)
        val p=prediction(gamestate)
        if (p.getOrDefault(Move.D,0.0)>P_THRESHOLD) {
            return Move.W
        }
        if (dynamiteLeft(gamestate) > 0 && pointsThisRound(gamestate) >= tar_draw) {
            if (p.getOrDefault(Move.W,0.0)<P_THRESHOLD){
                return Move.D
            }
        }
        rpsMoves.forEach{
            if (p.getOrDefault(it,0.0)>P_THRESHOLD){
                return rpsCounters[it]?:Move.R
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
    fun prediction(gamestate: Gamestate):Map<Move,Double>{
        //return best estimate of what the opponent is gonna do
        //STRANGE, ISN'T IT!
        val dataset=mutableMapOf<Int,MutableList<Move>>()
        gamestate.rounds.forEach {
            val points=pointsThatRound(gamestate,it)
            if (dataset.containsKey(points)){
                dataset[points]?.add(it.p2)
            }else{
                dataset[points]= mutableListOf(it.p2)
            }
        }
        var pts=pointsThisRound(gamestate)
        while (pts>1 && (dataset[pts]?.size?:0<CONFIDENCE_THRESHOLD)){
            pts--
        }
        if (!dataset.containsKey(pts)){
            return mapOf(Pair(Move.R,1.0))
        }
        val p= mutableMapOf<Move,Double>()
        val past_plays=dataset[pts]?: mutableListOf<Move>()
        allMoves.forEach{
            p[it]=Collections.frequency(past_plays,it)/past_plays.size.toDouble()
        }
        return p
    }
    fun gotCountered(gamestate: Gamestate): Boolean {
        //the opponent countered our dynamite play!
        return gamestate.rounds.any { it.p1 == Move.D && it.p2 == Move.W }
    }

    fun pointsThisRound(gamestate: Gamestate,onlyCount:Move?=null): Int {
        var points = 1
        for (round in gamestate.rounds.asReversed()) {
            if (round.p1 == round.p2 && (onlyCount==null || round.p1==onlyCount)) {
                points++
            } else {
                break
            }
        }
        return points
    }
    fun pointsThatRound(gamestate: Gamestate,round:Round):Int{
        val t=gamestate.rounds.indexOf(round)
        var pgs=Gamestate()
        pgs.rounds=gamestate.rounds.slice(0 until t)
        return pointsThisRound(pgs)
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