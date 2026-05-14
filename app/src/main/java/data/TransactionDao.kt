package com.example.nammasantheledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE name = :name LIMIT 1")
    suspend fun getCustomerByName(name: String): Customer?

    @Query("""
        SELECT c.name as customerName, 
               COALESCE(SUM(CASE WHEN t.type = 'GIVE' THEN t.amount ELSE 0 END), 0) as totalGive,
               COALESCE(SUM(CASE WHEN t.type = 'TAKE' THEN t.amount ELSE 0 END), 0) as totalTake,
               COALESCE(SUM(CASE WHEN t.type = 'GIVE' THEN t.amount ELSE -t.amount END), 0) as balance,
               c.phoneNumber, 
               c.photoUri
        FROM customers c
        LEFT JOIN transactions t ON c.name = t.customerName
        GROUP BY c.name
        ORDER BY balance DESC
    """)
    fun getCustomerBalances(): Flow<List<CustomerBalance>>

    @Query("""
        SELECT c.name as customerName, 
               SUM(CASE WHEN t.type = 'GIVE' THEN t.amount ELSE 0 END) as totalGive,
               SUM(CASE WHEN t.type = 'TAKE' THEN t.amount ELSE 0 END) as totalTake,
               SUM(CASE WHEN t.type = 'GIVE' THEN t.amount ELSE -t.amount END) as balance,
               c.phoneNumber, 
               c.photoUri
        FROM customers c
        LEFT JOIN transactions t ON c.name = t.customerName
        GROUP BY c.name
        HAVING balance > 0
    """)
    fun getDebtorsSync(): List<CustomerBalance>

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

data class CustomerBalance(
    val customerName: String,
    val totalGive: Double,
    val totalTake: Double,
    val balance: Double,
    val phoneNumber: String?,
    val photoUri: String?
) {
    @Ignore
    var transactions: List<Transaction> = emptyList()
}
