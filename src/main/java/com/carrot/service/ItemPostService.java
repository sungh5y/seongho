package com.carrot.service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.carrot.domain.CategoryVO;
import com.carrot.domain.ImageVO;
import com.carrot.domain.ItemPostVO;
import com.carrot.domain.SearchVO;
import com.carrot.domain.UserVO;
import com.carrot.repository.CategoryRepository;
import com.carrot.repository.ItemPostRepository;

@Service
public class ItemPostService {

    @Autowired
    private AWSS3 AWSS3;

    @Autowired
    private SqlSession sqlSession;

    @Autowired
    private ImageService imageService;
    private static HashMap<Integer, String> categoryMap;
    private static boolean isSetCategory;

    public int insert(UserVO user, ItemPostVO vo, List<MultipartFile> imageList) throws IOException {
        vo.setWriter(user.getId());
        vo.setCreated_at(new Date(System.currentTimeMillis()));
        if (sqlSession.getMapper(ItemPostRepository.class).insert(vo) > 0) {
            return setImage(vo, imageList);
        } else {
            return -1;
        }
    }

    public int setImage(ItemPostVO vo, List<MultipartFile> imageList) throws IOException {
        AWSS3.uploadImage(vo.getId(), imageList);
        for (int i = 0; i < imageList.size(); i++) {
            String url = "https://carrot-world.s3.ap-northeast-2.amazonaws.com/";
            String fileName = AWSS3.getFileName(vo.getId(), i, imageList.get(i));
            if (imageService.insert(new ImageVO(vo.getId(), i, url + fileName)) != 1) {
                return -1;
            }
        }
        return 1;
    }

    public List<ItemPostVO> search(SearchVO vo) {
        List<ItemPostVO> itemPostList = sqlSession.getMapper(ItemPostRepository.class).search(vo);
        if (!isSetCategory) {
            setCategoryMap();
        }
        if (itemPostList.isEmpty()) {
            return null;
        }
        return imageService.setFirstImage(itemPostList);
    }
    
    public List<ItemPostVO> selectByWriter(String writer) {
    	List<ItemPostVO> list = sqlSession.getMapper(ItemPostRepository.class).selectByWriter(writer);
    	System.out.println("list : " + list);
    	return list;
    }

    public ItemPostVO detail(int id) {
        ItemPostVO itemPost = sqlSession.getMapper(ItemPostRepository.class).selectById(id);
        List<ImageVO> imageList = imageService.selectById(id);
        if (!imageList.isEmpty()) {
            itemPost.setImageList(imageService.selectById(id));
        }
        if (!isSetCategory) {
            setCategoryMap();
        }
        itemPost.setCategory_name(categoryMap.get(itemPost.getCategory_id()));
        return itemPost;
    }

    public void setCategoryMap() {
        List<CategoryVO> categoryList = sqlSession.getMapper(CategoryRepository.class).selectAll();
        categoryMap = new HashMap<>();
        for (CategoryVO vo : categoryList) {
            categoryMap.put(vo.getId(), vo.getName());
        }
        isSetCategory = true;
    }

    public void addChatCnt(int postId) {
        sqlSession.getMapper(ItemPostRepository.class).addChatCnt(postId);
    }

    public int complete(ItemPostVO vo) {

        return sqlSession.getMapper(ItemPostRepository.class).updateComplete(vo);
    }
    public int delete(ItemPostVO vo) {
        return sqlSession.getMapper(ItemPostRepository.class).delete(vo);
    }
}
