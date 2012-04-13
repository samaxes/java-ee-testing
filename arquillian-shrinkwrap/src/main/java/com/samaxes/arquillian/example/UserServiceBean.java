package com.samaxes.arquillian.example;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class UserServiceBean {

    @PersistenceContext
    private EntityManager em;

    public User addUser(User user) {
        em.persist(user);
        return user;
    }

    // Does not need to open a transaction
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findUserById(Long id) {
        return em.find(User.class, id);
    }
}
