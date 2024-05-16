package jp.co.metateam.library.repository;

import java.util.List;
import java.util.Date;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jp.co.metateam.library.model.RentalManage;

@Repository
public interface RentalManageRepository extends JpaRepository<RentalManage, Long> {
    List<RentalManage> findAll();

	Optional<RentalManage> findById(Long id);

    @Query("SELECT COUNT(rm) FROM RentalManage rm " +
       " WHERE (rm.status = 0 OR rm.status = 1) " +
       " AND rm.id <> ?1" +
       " AND rm.stock.id = ?2") 
        Long countByStatusAndNotId(Long id,String stockid);


     @Query("SELECT COUNT(rm) FROM RentalManage rm " +
     " WHERE rm.stock.id = ?4 " +
     " AND rm.status IN (0, 1) " +
     " AND rm.id <> ?2 " +
     " AND (rm.expectedReturnOn < ?1 OR ?3 < rm.expectedRentalOn)")
           Long countByStatusAndExpectedReturnBeforeAndNotId(Date expectedRentalOn,Long id, Date expectedReturnOn, String stockid);
    
     @Query("SELECT COUNT(rm) FROM RentalManage rm" +
     " WHERE (rm.status= 0 OR rm.status =1)" +
     " AND rm.stock.id =?1")
     Long countByStatusAndStockId(String stockId);
     
     @Query("SELECT COUNT(rm) FROM RentalManage rm" +
     " WHERE rm.stock.id =?3" +
     " AND rm.status IN(0,1)" +
     " AND (rm.expectedReturnOn<?1 OR ?2 <rm.expectedRentalOn)")
     Long countByStockIdAndExpectedReturn(Date expectedRentalOn,Date expectedReturnOn,String stockid);
}
