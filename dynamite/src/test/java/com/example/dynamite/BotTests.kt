package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.softwire.dynamite.game.Round
import com.softwire.dynamite.runner.DynamiteRunner
import org.junit.Test

import org.junit.Assert.*

class BotTests {
    fun setup_gamestate(vararg movepairs:Pair<Move,Move>):Gamestate{
        val gs=Gamestate()
        gs.rounds= mutableListOf<Round>()
        movepairs.forEach {
            val newRound=Round()
            newRound.p1=it.first
            newRound.p2=it.second
            gs.rounds.add(newRound)
        }
        return gs
    }
    @Test fun beats_all_example_bots(){
        val results = DynamiteRunner.playGames(DynamiteRunner.Factory<Bot>{MyBot()}).results
        results.forEach {
            assertEquals("WIN",it.result)
        }
    }
    @Test fun dynamites_on_long_draw_chain() {
        val bot = MyBot()
        val gamestate = setup_gamestate(
            Pair(Move.P, Move.P), Pair(Move.P, Move.P), Pair(Move.R, Move.R),
            Pair(Move.S, Move.S)
        )
        assertEquals(Move.D, bot.makeMove(gamestate))
    }
}