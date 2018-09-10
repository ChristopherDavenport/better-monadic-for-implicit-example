package com.example.quickstart

import cats.effect.{ConcurrentEffect, Effect, ExitCode, IO, IOApp, Timer, Sync, ContextShift}
import cats.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import fs2.Stream
import scala.concurrent.ExecutionContext.global

import org.http4s.server.middleware.Logger

object Server {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, ExitCode] =
    for {
      client <- BlazeClientBuilder[F](global).stream
      implicit0(helloWorldAlg) = HelloWorldAlg.impl[F]
      implicit0(jokeAlg) <- Stream.pure(JokeAlg.impl[F](client)).covary[F]

      // Combine Service Routes into an HttpApp
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        Edge.helloWorldRoutes[F] <+>
        Edge.jokeRoutes[F]
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger(true, true)(httpApp)


      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
}