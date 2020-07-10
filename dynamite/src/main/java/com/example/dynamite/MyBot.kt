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
    val P_THRESHOLD=0.1
    val rpsMoves = listOf(Move.R, Move.P, Move.S)
    val allMoves= listOf(Move.R,Move.P,Move.S,Move.W,Move.D)
    val r_cache =
        mutableMapOf<Pair<Int, Int>, List<Int>>() //so we don't make a new list every ranInt call
    val dataset=mutableMapOf<Int,MutableList<Move>>()
    val CHECK_PROGRESS=1000 //check score at this point
    var erratic=false //go crazy if we're losing
    val myopia=2000
    override fun makeMove(gamestate: Gamestate): Move {
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move
        val tar_draw=target_draw_number(gamestate)
        val p=prediction(gamestate)
        val dynamite=dynamiteLeft(gamestate) > 0 && pointsThisRound(gamestate) >= ranInt(tar_draw-1,tar_draw)
        var counters=counter_prediction(p,dynamite)
        val sortedP=counters.values.sorted()
        val mx=sortedP.last()
        var predicted=rpsMoves.shuffled().first()
        if (mx-sortedP[sortedP.size-2]>P_THRESHOLD) {
            return counters.maxBy { it.value }?.key?:Move.R
        }
        if (erratic && ranInt(1)==1){
            //they're gonna counter us, counter them back!
            return counter(counter(counter(predicted,dynamite)))
        }
        return counter(predicted,dynamite)
    }
    fun counter(p2Move:Move,dynamite:Boolean=false):Move{
        if (dynamite && rpsMoves.contains(p2Move)){
            return Move.D
        }
        return when(p2Move){
            Move.D->Move.W
            Move.W->rpsMoves.shuffled().first()
            Move.S->Move.R
            Move.R->Move.P
            Move.P->Move.S
        }
    }
    fun counter_prediction(p:Map<Move,Double>,dynamite: Boolean):Map<Move,Double>{
        val cp= mutableMapOf<Move,Double>()
        for (m in allMoves){
            cp[m]=0.0
        }
        p.forEach{
            cp.plusAssign(Pair(counter(it.key,dynamite),it.value))
        }
        return cp
    }
    fun dynamiteLeft(gamestate: Gamestate): Int {
        return 100 - gamestate.rounds.map { if (it.p1 == Move.D) 1 else 0 }.sum()
    }
    fun oppositionDynamite(gamestate: Gamestate):Int{
        return 100 - gamestate.rounds.map { if (it.p2 == Move.D) 1 else 0 }.sum()
    }
    fun target_draw_number(gamestate: Gamestate):Int{
        //return how many draws we need for optimal dynamite conservation
        val dynamite_fraction=dynamiteLeft(gamestate)/kotlin.math.max(2000.0-gamestate.rounds.size,1.0)
        if (dynamite_fraction==0.0){return 9999}
        else{
            var n=0
            while (0.33333.pow(n)>dynamite_fraction){
                n++
            }
            return n
        }

    }
    fun add_data(gamestate: Gamestate,vararg rounds:Round){
        rounds.forEach {
            val points=pointsThatRound(gamestate,it)
            if (dataset.containsKey(points)){
                dataset[points]?.add(it.p2)
                if (dataset[points]?.size?:0>myopia){
                    dataset[points]?.removeAt(0)
                }
            }else{
                dataset[points]= mutableListOf(it.p2)
            }
        }
    }
    fun prediction(gamestate: Gamestate):Map<Move,Double>{
        //return best estimate of what the opponent is gonna do
        //STRANGE, ISN'T IT!
        if (gamestate.rounds.isEmpty()){
            return allMoves.map { Pair(it,0.2) }.toMap()
        }
        add_data(gamestate,gamestate.rounds.last())
        var pts=pointsThisRound(gamestate)
        while (pts>1 && (dataset[pts]?.size?:0<CONFIDENCE_THRESHOLD)){
            pts--
        }
        val p= mutableMapOf<Move,Double>()
        val past_plays=dataset[pts]?: mutableListOf<Move>()
        allMoves.forEach{
            p[it]=Collections.frequency(past_plays,it)/past_plays.size.toDouble()
        }
        if (oppositionDynamite(gamestate)==0){
            p[Move.D]=0.0
        }
        return p
    }
    fun winning(gamestate: Gamestate):Boolean{
        val scores= mutableMapOf(Pair(1,0), Pair(2,0))
        gamestate.rounds.forEach {
            val winner=get_winner(it)
            if (winner>0){
                scores[winner]=scores.getOrDefault(winner,0)+pointsThatRound(gamestate,it)
            }
        }
        return scores.getOrDefault(1,0)>=scores.getOrDefault(2,0)
    }
    fun get_winner(round: Round):Int{
        if (round.p1==round.p2){
            return 0
        }else if (round.p1==Move.W){
            return if (round.p2==Move.D) 1 else 2
        }else if (round.p2==Move.W){
            return if (round.p1==Move.D) 2 else 1
        }else{
            return if (round.p1==counter(round.p2)) 1 else 2
        }
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
        dataset.clear()
        erratic=false
    }
}