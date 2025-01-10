package dincyclopedia.model

import cats.Eq
import cats.derived.*

case class Skill(
    power: Int,                        //		0
    usesComboPoints: Boolean,          //	0
    requiredEnemyStatus: String,       //	Normal
    enemyEvent: String,                //	None
    skillType: String,                 //	DirectDamage
    basePriority: Double,              //	0.5
    damageType: String,                //		Physical
    projDamageType: String,            //		Physical
    explosionDamage: String,           // 	Physical
    cureEffectDamageType: String,      //	Physical
    baseCost: Int,                     //		1
    costIncreasePerLevel: Int,         //	1
    statReq: String,                   //		None
    effectTime: Int,                   //	5
    silentHateChange: Double,          //	0.0
    comparePointsMult: Double,         //	1.0
    hardCodedLocation: Boolean,        //	0
    upgradeItemType: String,           //		Other
    waitForAnim: Boolean,              //	1
    showSkillHint: Boolean,            //	1
    maxUserRarity: String,             //	Unique11
    zombieInfectionChanceMult: Double, //	1.0
    maxRange: Double,
    maxRangeDamageMult: Double,     //	1.0
    maxRangeFinishingExtra: Double, //	40.0
    needsEnemy: Boolean,
    self: Boolean,
    needsFriend: Boolean,
    passive: Boolean,
    stats: Map[String, ScalingStat],
    statInflation: Double,
) extends Entry
    derives Eq

object Skill extends JsonStorage {
  override val filename = "skills"
}
