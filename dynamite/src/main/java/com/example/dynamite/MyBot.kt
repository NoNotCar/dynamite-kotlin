package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.softwire.dynamite.game.Round
import java.util.*
import kotlin.math.pow
import kotlin.random.Random
enum class Movetype{
    RPS,D,W
}
class MyBot : Bot {
    val CONFIDENCE_THRESHOLD=1 //size of past moves before we're confident
    val P_THRESHOLD=0.1
    val rpsMoves = listOf(Move.R, Move.P, Move.S)
    val allMoves= listOf(Move.R,Move.P,Move.S,Move.W,Move.D)
    val r_cache =
        mutableMapOf<Pair<Int, Int>, List<Int>>() //so we don't make a new list every ranInt call
    val dataset=mutableMapOf<Pair<Int,Movetype>,MutableList<Move>>()
    val CHECK_PROGRESS=1000 //check score at this point
    var erratic=false //go crazy if we're losing
    val myopia=100
    val DISCOUNT_RATE=0.9
    fun toMoveType(move:Move):Movetype{
        return when(move){
            Move.D->Movetype.D
            Move.W->Movetype.W
            else->Movetype.RPS
        }
    }
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
    fun geosum(n:Int):Double{
        return (1-DISCOUNT_RATE.pow(n))/(1-DISCOUNT_RATE)
    }
    fun counter_prediction(p:Map<Move,Double>,dynamite: Boolean):Map<Move,Double>{
        val cp= mutableMapOf<Move,Double>()
        for (m in allMoves){
            cp[m]=0.0
        }
        p.forEach{
            val c=counter(it.key,dynamite)
            cp.put(c,cp.getOrDefault(c,0.0)+it.value)
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
        rounds.withIndex().forEach {
            val points=pointsThatRound(gamestate,it.value)
            val ourPrevMove=toMoveType(if (it.index==0) Move.S else rounds[it.index-1].p1)
            if (dataset.containsKey(Pair(points,ourPrevMove))){
                dataset[Pair(points,ourPrevMove)]?.add(it.value.p2)
                if (dataset[Pair(points,ourPrevMove)]?.size?:0>myopia){
                    dataset[Pair(points,ourPrevMove)]?.removeAt(0)
                }
            }else{
                dataset[Pair(points,ourPrevMove)]= mutableListOf(it.value.p2)
            }
        }
    }
    fun discounted_freq(list: List<Any>,what:Any?=null):Double{
        var f=0.0
        for (l in list.asReversed().withIndex()){
            if (l.value==what || what==null){
                f+=DISCOUNT_RATE.pow(l.index)
            }
        }
        return f
    }
    fun prediction(gamestate: Gamestate):Map<Move,Double>{
        //return best estimate of what the opponent is gonna do
        //STRANGE, ISN'T IT!
        if (gamestate.rounds.isEmpty()){
            return allMoves.map { Pair(it,0.2) }.toMap()
        }
        add_data(gamestate,gamestate.rounds.last())
        var pts=pointsThisRound(gamestate)
        val ourPrevMove=toMoveType(gamestate.rounds.last().p1)
        while (pts>1 && (dataset[Pair(pts,ourPrevMove)]?.size?:0<CONFIDENCE_THRESHOLD)){
            pts--
        }
        val p= mutableMapOf<Move,Double>()
        val past_plays=dataset[Pair(pts,ourPrevMove)]?: mutableListOf<Move>()
        allMoves.forEach{
            p[it]=discounted_freq(past_plays,it)/geosum(past_plays.size)
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