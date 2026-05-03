package com.pondersource.solidcontacts.repository.user

interface UserRepository {

    fun getGrantedWebId(): String

    fun setGrantedWebId(webId: String)
}