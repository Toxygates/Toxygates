/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.platform

object Species extends Enumeration(0) {
  type Species = Value
  val Human, Rat, Mouse = Value

  implicit class ExtSpecies(s: Species) {
    def longName: String = s match {
      case Human => "Homo sapiens"
      case Rat   => "Rattus norvegicus"
      case Mouse => "Mus musculus"
    }
    def taxon: Int = s match {
      case Human => 9606
      case Rat   => 10116
      case Mouse => 10090
    }
    def shortCode: String = s match {
      case Human => "hsa"
      case Rat   => "rno"
      case Mouse => "mmu"
    }
  }

  val supportedSpecies = List(Rat, Human, Mouse)
}