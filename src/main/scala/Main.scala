package com.github.plippe.suprnation

import cats.MonadError
import cats.implicits._
import cats.data.NonEmptyList
import cats.effect.IO

import com.github.plippe.suprnation.io._

object Main {

  def main(args: Array[String]): Unit = {
    val reader = new StandardInputReader[IO]()
    val writer = new StandardOutputWriter[IO]()

    val parser = new IntParser[IO]()
    val combiner = new TreeCombiner[Int, IO]()
    val solver = new ShortestPathSolver[Int, IO](parser, combiner)

    val output = { solution: NonEmptyList[Int] =>
      s"Minimal path is: ${solution.toList.mkString(" + ")} = ${solution.toList.sum}"
    }

    build(reader, writer, solver, output).unsafeRunSync
  }

  def build[T, V, F[_]](reader: Reader[F],
                        writer: Writer[F],
                        solver: Solver[T, V, F],
                        output: NonEmptyList[T] => String)(
      implicit F: MonadError[F, Throwable]): F[Unit] = {

    def readAll(reader: Reader[F],
                list: List[String]): F[NonEmptyList[String]] = {
      reader.readLine().flatMap {
        case Some(line) => readAll(reader, list :+ line)
        case None =>
          NonEmptyList
            .fromList(list)
            .fold(F.raiseError[NonEmptyList[String]](ParserEmptyLine))(
              _.pure[F])
      }
    }

    val f = for {
      problem <- readAll(reader, List.empty)
      solution <- solver.solve(problem)
      _ <- writer.writeLine(output(solution))
    } yield ()

    f
  }

}
