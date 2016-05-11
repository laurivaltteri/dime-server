/*
  Copyright (c) 2016 University of Helsinki

  Permission is hereby granted, free of charge, to any person
  obtaining a copy of this software and associated documentation files
  (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge,
  publish, distribute, sublicense, and/or sell copies of the Software,
  and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be
  included in all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
*/

package fi.hiit.dime.database;

import fi.hiit.dime.data.Profile;
import fi.hiit.dime.authentication.User;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

// interface ProfileRepositoryCustom {
//     public Profile replace(Profile oldProfile, Profile newProfile);
// }

// abstract class ProfileRepositoryImpl implements ProfileRepositoryCustom {
//     @PersistenceContext
//     protected EntityManager entityManager;

//     @Override
//     public Profile replace(Profile oldProfile, Profile newProfile) {
//         newProfile.copyIdFrom(oldProfile);
//         return entityManager.merge(newProfile);
//     }
// }

public interface ProfileRepository extends CrudRepository<Profile, Long> {
                                           // ProfileRepositoryCustom {
    Profile findOne(Long id);

    Profile findOneByIdAndUser(Long id, User user);

    List<Profile> findByUser(User user);
}
