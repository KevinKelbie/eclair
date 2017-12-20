package fr.acinq.eclair.db.sqlite

import java.sql.Connection

import fr.acinq.bitcoin.BinaryData
import fr.acinq.eclair.db.{Payment, PaymentsDb}

/**
  * Payments are stored in the `payments` table.
  * The primary key in this DB is the `payment_hash` column. Columns are not nullable.
  * <p>
  * Types:
  * <ul>
  * <li>`payment_hash`: BLOB
  * <li>`amount_msat`: INTEGER
  * <li>`timestamp`: INTEGER (unix timestamp)
  */
class SqlitePaymentsDb(sqlite: Connection) extends PaymentsDb {

  {
    val statement = sqlite.createStatement
    statement.executeUpdate("CREATE TABLE IF NOT EXISTS payments (payment_hash BLOB NOT NULL PRIMARY KEY, amount_msat INTEGER NOT NULL, timestamp INTEGER NOT NULL)")
  }

  override def addPayment(payment: Payment): Unit = {
    val statement = sqlite.prepareStatement("INSERT INTO payments VALUES (?, ?, ?)")
    statement.setBytes(1, payment.payment_hash)
    statement.setLong(2, payment.amount_msat)
    statement.setLong(3, payment.timestamp)
    statement.executeUpdate()
  }

  @throws(classOf[NoSuchElementException])
  override def findByPaymentHash(paymentHash: BinaryData): Payment = {
    val statement = sqlite.prepareStatement("SELECT payment_hash, amount_msat, timestamp FROM payments WHERE payment_hash = ?")
    statement.setBytes(1, paymentHash)
    val rs = statement.executeQuery()
    if (rs.next()) {
      Payment(BinaryData(rs.getBytes("payment_hash")), rs.getLong("amount_msat"), rs.getLong("timestamp"))
    } else {
      throw new NoSuchElementException("payment not found")
    }
  }

  override def listPayments(): List[Payment] = {
    val rs = sqlite.createStatement.executeQuery("SELECT payment_hash, amount_msat, timestamp FROM payments")
    var l: List[Payment] = Nil
    while (rs.next()) {
      l = l :+ Payment(BinaryData(rs.getBytes("payment_hash")), rs.getLong("amount_msat"), rs.getLong("timestamp"))
    }
    l
  }

}
