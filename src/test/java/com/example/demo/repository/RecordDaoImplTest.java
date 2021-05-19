package com.example.demo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.example.demo.entity.Record;

import java.time.LocalDateTime;

@SpringJUnitConfig
@SpringBootTest
@ActiveProfiles("unit")
@Sql
class RecordDaoImplTest {

    @Autowired
    private RecordDaoImpl recordDao;
    
    @Test
    @DisplayName("findAllのテスト")
    void findAll() {
        var list = recordDao.findAll();

        // 件数のチェック
        assertEquals(6, list.size());

        // 2件目のレコードの取得(ORDER BYが正しく反映されているか)
        var record2 = list.get(1);
        assertNotNull(record2);
        

        // 各カラムの値が正しくセットされているか
        assertEquals("よく食べたてすと", record2.getComment());
        assertEquals("ddddtest", record2.getRecPic());
        assertEquals(LocalDateTime.of(2021, 03, 13, 15, 00, 00), record2.getRecDate());
        assertEquals(3, record2.getPetId());
        
        var record3 = list.get(2);
        assertNotNull(record3);

        assertEquals("歌っているてすと", record3.getComment());

    }
    
    @Test
    @DisplayName("findByRecIdのテスト(正常系)")
    void findByRecId1() {
        var rec1 = recordDao.findByRecId(1);

        // レコードの存在チェック
        assertNotNull(rec1);

        // 各カラムの値が正しくセットされているか
        assertEquals("今日も元気てすと", rec1.getComment());
        assertEquals("cccctest", rec1.getRecPic());
        assertEquals(LocalDateTime.of(2021, 03, 12, 15, 00, 00), rec1.getRecDate());
        assertEquals(1, rec1.getPetId());
    }

    @Test
    @DisplayName("findByRecIdのテスト(レコードが取得できない場合)")
    void findByRecId2() {
        // レコードが取得できず例外がスローされるか
        assertThrows(EmptyResultDataAccessException.class, () -> recordDao.findByRecId(10));
    }
    
    @Test
    @DisplayName("findByPetId(正常系)のテスト")
    void findByPetId1() {
        var list = recordDao.findByPetId(2);
        // 件数のチェック
        assertEquals(2, list.size());

        // 1件目のレコードの取得(ORDER BYが正しく反映されているか)
        var rec2 = list.get(0);
        assertNotNull(rec2);

        // 各カラムの値が正しくセットされているか
        assertEquals(3, rec2.getRecId());
        assertEquals("歌っているてすと", rec2.getComment());
        assertEquals("eeeetest", rec2.getRecPic());
        assertEquals(LocalDateTime.of(2021, 03, 14, 15, 00, 00), rec2.getRecDate());
        assertEquals(2, rec2.getPetId());

    }
    
    @Test
    @DisplayName("findByUserId正常系)のテスト")
    void findByUserId1() {
        var list = recordDao.findByUserId(1);
        // 件数のチェック
        assertEquals(2, list.size());

        // 1件目のレコードの取得(ORDER BYが正しく反映されているか)
        var rec2 = list.get(0);
        assertNotNull(rec2);

        // 各カラムの値が正しくセットされているか
        assertEquals(1, rec2.getRecId());
        assertEquals("今日も元気てすと", rec2.getComment());
        assertEquals("cccctest", rec2.getRecPic());
        assertEquals(LocalDateTime.of(2021, 03, 12, 15, 00, 00), rec2.getRecDate());
        assertEquals(1, rec2.getPetId());

    }
    
    
    
    @Test
    @DisplayName("insertのテスト(正常系)")
    void insert() {    	
        var rec = new Record();
        rec.setRecId(0);					//何でゼロ？7じゃなくて？
        rec.setComment("一緒に散歩したてすと4");
        rec.setRecPic("ggggtest4");
        rec.setRecDate(LocalDateTime.of(2007, 12, 03, 10, 15, 30));
        rec.setPetId(1);

        recordDao.insert(rec);

        var list = recordDao.findAll();

        // 件数のチェック
        assertEquals(7, list.size());

        // 登録されたレコードの取得
        var recx = list.get(6);

        // 各カラムの値が正しくセットされているか
        assertEquals(rec.getRecId(), recx.getRecId());
        assertEquals(rec.getComment(), recx.getComment());
        assertEquals(rec.getRecPic(), recx.getRecPic());
        assertEquals(rec.getRecDate(), recx.getRecDate());
        assertEquals(rec.getPetId(), recx.getPetId());
        
    }
    

    @Test
    @DisplayName("updateのテスト(正常系)")
    void update1() {
        var rec = new Record();
        rec.setRecId(3);
        rec.setComment("よく食べたてすとx");
        rec.setRecPic("ddddtestx");
        rec.setRecDate(LocalDateTime.of(2007, 12, 03, 10, 15, 30));
        rec.setPetId(3);

        var updateCount = recordDao.update(rec);

        assertEquals(1, updateCount);

        var rec2 = recordDao.findByRecId(3);

        // レコードの存在チェック
        assertNotNull(rec2);

        // 各カラムの値が正しくセットされているか
        assertEquals(3, rec2.getRecId());
        assertEquals("よく食べたてすとx", rec2.getComment());
        assertEquals("ddddtestx", rec2.getRecPic());
        assertEquals(LocalDateTime.of(2007, 12, 03, 10, 15, 30), rec2.getRecDate());
        assertEquals(3, rec2.getPetId());
    }

    @Test
    @DisplayName("updateのテスト(更新対象がない場合)")
    void update2() {
        var rec = new Record();
        rec.setRecId(10);
        var updateCount = recordDao.update(rec);
        assertEquals(0, updateCount);
    }

    @Test
    @DisplayName("deleteByRecIdのテスト(正常系)")
    void deleteByRecId1() {
    	recordDao.deleteByRecId(1);

        var list = recordDao.findAll();

        // 件数のチェック(対象外のレコードまで消えていないかチェック)
        assertEquals(5, list.size());

        // レコードが取得できないことを確認
        assertThrows(EmptyResultDataAccessException.class, () -> recordDao.findByRecId(1));
    }

    @Test
    @DisplayName("deleteByRecIdのテスト(更新対象がない場合)")
    void deleteByRecId2() {
        var deleteCount = recordDao.deleteByRecId(10);
        assertEquals(0, deleteCount);

        var list = recordDao.findAll();

        // 件数のチェック(全てのレコードが消えていない事を確認)
        assertEquals(6, list.size());

    }

    @Test
    @DisplayName("deleteByPetIdのテスト(正常系)")
    void deleteByPetId1() {
    	recordDao.deleteByPetId(2);

        var list = recordDao.findAll();

        // 件数のチェック(対象外のレコードまで消えていないかチェック)
        assertEquals(4, list.size());

        // レコードが取得できないことを確認
        assertThrows(EmptyResultDataAccessException.class, () -> recordDao.findByRecId(3));
    }

    @Test
    @DisplayName("deleteByPetIdのテスト(更新対象がない場合)")
    void deleteByPetId2() {
        var deleteCount = recordDao.deleteByPetId(10);
        assertEquals(0, deleteCount);

        var list = recordDao.findAll();

        // 件数のチェック(全てのレコードが消えていない事を確認)
        assertEquals(6, list.size());

    }
    
    @Test
    @DisplayName("deleteByUserIdのテスト(正常系)")
    void deleteByUserId1() {
    	recordDao.deleteByUserId(1);

        var list = recordDao.findAll();

        // 件数のチェック(対象外のレコードまで消えていないかチェック)
        assertEquals(2, list.size());

        // レコードが取得できないことを確認
        assertThrows(EmptyResultDataAccessException.class, () -> recordDao.findByRecId(1));
    }

    @Test
    @DisplayName("deleteByUserIdのテスト(更新対象がない場合)")
    void deleteByUserId2() {
        var deleteCount = recordDao.deleteByUserId(10);
        assertEquals(0, deleteCount);

        var list = recordDao.findAll();

        // 件数のチェック(全てのレコードが消えていない事を確認)
        assertEquals(6, list.size());

    }
    
}