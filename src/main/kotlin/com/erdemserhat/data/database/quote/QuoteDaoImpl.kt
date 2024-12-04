package com.erdemserhat.data.database.quote

import com.erdemserhat.models.Quote
import com.erdemserhat.data.database.DatabaseConfig
import com.erdemserhat.data.database.liked_quotes.DBLikedQuoteTable
import com.erdemserhat.data.database.quote_category.DBQuoteCategoryTable
import com.erdemserhat.models.QuoteResponse
import com.erdemserhat.routes.quote.get_quotes.FilteredQuoteRequest
import org.jetbrains.exposed.sql.functions.math.PiFunction.expr
import org.ktorm.dsl.*
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.support.mysql.rand
import kotlin.random.Random

class QuoteDaoImpl : QuoteDao {
    override suspend fun addQuote(quote: Quote): Int {
        return DatabaseConfig.ktormDatabase.insert(DBQuoteTable) {
            set(DBQuoteTable.quote, quote.quote)
            set(DBQuoteTable.writer, quote.writer)
            set(DBQuoteTable.imageUrl, quote.imageUrl)
            set(DBQuoteTable.quoteCategoryId, quote.quoteCategory)

        }
    }

    override suspend fun deleteQuoteById(id: Int): Int {
        return DatabaseConfig.ktormDatabase.delete(DBQuoteTable) {
            DBQuoteTable.id eq id

        }
    }

    override suspend fun deleteAll(): Int {
        // Delete a category from the database
        return DatabaseConfig.ktormDatabase.deleteAll(DBQuoteTable)

    }

    override suspend fun updateQuote(quote: Quote): Boolean {
        try {
            DatabaseConfig.ktormDatabase.update(DBQuoteTable) {
                set(DBQuoteTable.quote, quote.quote)
                set(DBQuoteTable.writer, quote.writer)
                set(DBQuoteTable.imageUrl, quote.imageUrl)
                set(DBQuoteTable.quoteCategoryId, quote.quoteCategory)
                where {
                    DBQuoteTable.id eq quote.id
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }

    }

    override suspend fun getQuotes(): List<DBQuoteEntity> {
        return DatabaseConfig.ktormDatabase.sequenceOf(DBQuoteTable).toList()
    }


    override suspend fun getCategoriesWithPagination(
        page: Int,
        pageSize: Int,
        categoryIds: List<Int>,
        seed: Int,
        userId: Int
    ): List<QuoteResponse> {
        val quotesPerCategory = pageSize / categoryIds.size
        val startPoint = (page - 1) * quotesPerCategory
        val database = DatabaseConfig.ktormDatabase

        return database.useConnection { conn ->
            val results = mutableListOf<QuoteResponse>()

            for (categoryId in categoryIds) {
                val sql = if (categoryId == -1) {
                    // Beğenilen alıntılar için sorgu
                    """
                SELECT id, quote, writer, image_url, quote_category_id
                FROM quotes
                WHERE id IN (
                    SELECT quote_id 
                    FROM liked_quote 
                    WHERE user_id = ?
                )
                ORDER BY RAND(?)
                LIMIT ? OFFSET ?
                """
                } else {
                    // Diğer kategoriler için sorgu
                    """
                SELECT id, quote, writer, image_url, quote_category_id
                FROM quotes
                WHERE quote_category_id = ?
                ORDER BY RAND(?)
                LIMIT ? OFFSET ?
                """
                }

                conn.prepareStatement(sql).use { statement ->
                    if (categoryId == -1) {
                        // Beğenilen alıntılar için parametreler
                        statement.setInt(1, userId)
                        statement.setInt(2, seed)
                        statement.setInt(3, quotesPerCategory)
                        statement.setInt(4, startPoint)
                    } else {
                        // Diğer kategoriler için parametreler
                        statement.setInt(1, categoryId)
                        statement.setInt(2, seed)
                        statement.setInt(3, quotesPerCategory)
                        statement.setInt(4, startPoint)
                    }

                    statement.executeQuery().use { rs ->
                        generateSequence {
                            if (rs.next()) {
                                QuoteResponse(
                                    id = rs.getInt("id"),
                                    quote = rs.getString("quote"),
                                    writer = rs.getString("writer"),
                                    imageUrl = rs.getString("image_url"),
                                    quoteCategory = rs.getInt("quote_category_id"),
                                    isLiked = categoryId == -1 // Beğenilen alıntılar için true
                                )
                            } else null
                        }.toList().let { results.addAll(it) }
                    }
                }
            }

            return@useConnection results.shuffled(Random(seed))
        }
    }







}

infix fun Int.customXor(other: Int): Int {
    return this xor other
}
