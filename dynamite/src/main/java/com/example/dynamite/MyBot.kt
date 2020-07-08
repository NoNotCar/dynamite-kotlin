package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import kotlin.random.Random

class MyBot : Bot {
    override fun makeMove(gamestate: Gamestate): Move {
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move
        if (dynamiteLeft(gamestate)>0 && pointsThisRound(gamestate)>=2){
            return Move.D
        }
        return arrayOf(Move.R,Move.P,Move.S)[Random.nextInt(3)]
    }
    fun dynamiteLeft(gamestate: Gamestate):Int{
        return 100-gamestate.rounds.map { if (it.p1==Move.D) 1 else 0 }.sum()
    }
    fun pointsThisRound(gamestate: Gamestate):Int{
        var points=0
        for (round in gamestate.rounds.asReversed()){
            if (round.p1==round.p2){
                points++
            }else{
                break
            }
        }
        return points
    }
    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        println("Started new match")
    }
}