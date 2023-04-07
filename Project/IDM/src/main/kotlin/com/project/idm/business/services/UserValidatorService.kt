package com.project.idm.business.services

import com.project.idm.data.entities.Authority
import com.project.idm.data.entities.User
import com.project.idm.data.dtos.UserDTO
import com.project.idm.persistence.repositories.AuthorityRepository
import com.project.idm.persistence.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate


@Service
//class UserValidatorService : IUserValidatorService {
class UserValidatorService  {

//    override fun checkUserFieldsValidity(userModel: UserModel): String {
//        return ""
//    }

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var authorityRepository: AuthorityRepository

    fun checkUserFieldsValidity(userDTO: UserDTO): Boolean {

        val username = userDTO.getUsername()
        val password = userDTO.getPassword()
        val passwordConfirm = userDTO.getPasswordConfirm()

        val validUsername = username.length in 2..20
        val validPassword = password == passwordConfirm && password.length > 2
        val validFields = validUsername && validPassword

        return validFields
    }

    fun isUserRegisterValid(userDTO: UserDTO): Boolean {

        // 1. validate fields
        val validFields = checkUserFieldsValidity(userDTO)
        if (!validFields) return false

        // IMPORTANT: can't insert user before making sure in both DBs we can insert it!
        // that's why we check before posting
        val username = userDTO.getUsername()

        // 2. check if it's already in db (IDM Service)
        val userFound = userRepository.findUserByUsername(username)
        if (userFound.isPresent) return false

        // todo
        // 3. check if it's already in db (Profile Service)

        // todo
        // 4. post in IDM Service and Profile Service the User and Profile
        // perform logic to register User here + UserProfle
        try {
            // post in IDM Service
            val passwordEncoder = BCryptPasswordEncoder()
            val hashedPassword = passwordEncoder.encode(userDTO.getPassword())

            val authorities = mutableSetOf<Authority>()
            val readAuthority = authorityRepository.findAuthorityByName("read")
            authorities.add(readAuthority)

            val user = User()
            user.setUsername(userDTO.getUsername())
            user.setPassword(hashedPassword)
            user.setAuthorities(authorities)

            userRepository.save(user)

            println(userDTO.getUsername())
            println(userDTO.getPassword())
            println(userDTO.getPasswordConfirm())

            //todo
            // post in Profile Service
            val requestUrl = "http://localhost:8002/hello"
            sendRequest(requestUrl, userDTO)

        } catch (exception: Exception) {
            println("$exception")
            return false
        }

        return true
    }


    fun sendRequest(url: String, userDTO: UserDTO) {
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val request = HttpEntity(userDTO, headers)
        val response = restTemplate.postForObject(url, request, String::class.java)

        println("Response: $response")
    }

}