package org.seckill.service.impl;

import java.util.Date;
import java.util.List;

import org.seckill.dao.SeckillDao;
import org.seckill.dao.SuccessKilledDao;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.entity.SuccessKilled;
import org.seckill.enums.SeckillStatEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.seckill.service.SeckillService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

/**
 * 
 * @author Peter 2016-9-2下午2:39:05
 * 
 */
@Service
public class SeckillServiceImpl implements SeckillService {
//	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private SeckillDao seckillDao;
	@Autowired
	private SuccessKilledDao successKilledDao;
	// md5字符串，用于混淆md5
	private final String slat = "jdsfjojf*&%&^%^&$%^fbsjhyfsu";

	@Override
	public List<Seckill> getSeckillList() {
		return seckillDao.queryAll(0, 50);
	}

	@Override
	public Seckill getById(long seckillId) {
		return seckillDao.queryById(seckillId);
	}

	@Override
	/**
	 * 获取秒杀商品开启或者未开启信息
	 * @param seckillId
	 */
	public Exposer exportSeckillUrl(long seckillId) {
		Seckill seckill = seckillDao.queryById(seckillId);
		if (seckill == null) {//秒杀商品不存在
			return new Exposer(false, seckillId);
		}
		//获取秒杀商品开启时间，结束时间，系统时间
		Date startTime = seckill.getStartTime();
		Date endTime = seckill.getEndTime();
		Date nowTime = new Date();
		//判断秒杀是否开启
		if (nowTime.getTime() < startTime.getTime()
				|| nowTime.getTime() > endTime.getTime()) {//秒杀未开启
			return new Exposer(false, seckillId,nowTime.getTime(), startTime.getTime(),
					endTime.getTime());
		}
		//秒杀开启
		String md5 = getMD5(seckillId);// 转化特定字符串的过程，不可逆
		return new Exposer(true, md5, seckillId);
	}

	private String getMD5(long seckillId) {
		String base = seckillId + "/" + slat;
		String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
		return md5;
	}

	@Override
	@Transactional
	/**
	 * 使用注解来控制事物方法的有点
	 * 1.开发团队达成一致约定，明确标注解事务方法的编程风格
	 * 2.保证事物方法的执行时间尽可能短，不要穿插其他网络操作
	 * 3.不是所有的方法都需要事务，如只有一条修改操作，只读操作不需要事务控制
	 */
	public SeckillExecution executeSeckill(long seckillId, long userPhone,
			String md5) throws SeckillException, SeckillCloseException,
			RepeatKillException {
		if (md5 == null || !md5.equals(getMD5(seckillId))) {
			throw new SeckillException("seckill data rewrite");
		}
		// 执行秒杀逻辑：减库存+记录购买行为
		Date nowTime = new Date();
		try {
		// 减库存
			System.out.println("事务1update锁住前number"+seckillDao.queryById(seckillId).getNumber());
		int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
		    System.out.println("事务1update锁住后number"+seckillDao.queryById(seckillId).getNumber());
		if(updateCount<=0){
			//没有更新，秒杀结束
			throw new SeckillCloseException("seckill is closed");
		}else{
			
			for(int i=1;i<10;i++){
				Thread.sleep(1000);
				System.out.println("1执行完upadate语句:没有insert"+(i+1)*1000);
			}
			
			//记录购买行为
			int insertCount=successKilledDao.insertSuccessKilled(seckillId, userPhone);
			System.out.println("1执行完insert语句==================");
			//唯一seckillId+userPhone,检查重复
			if(insertCount<=0){
				//重复秒杀
				throw new RepeatKillException("seckill repeated");
			}else{
				//秒杀成功
				SuccessKilled successKilled=successKilledDao.queryByIdWithSeckill(seckillId,userPhone);
				System.out.println("1执行完秒杀成功==================");
				return new SeckillExecution(seckillId,SeckillStatEnum.SUCCESS, successKilled);			
			}
		}
		}catch(SeckillCloseException e1){
			throw e1;
		}catch(RepeatKillException e2){
			throw e2;
		} catch (Exception e) {
//			logger.error(e.getMessage(),e);
			//所有编译器异常转化成运行异常
			throw new SeckillException("seckill inner error:"+e.getMessage());
		}
	}

	@Transactional
	public SeckillExecution executeSeckill1(long seckillId, long userPhone,
			String md5) throws SeckillException, SeckillCloseException,
			RepeatKillException {
		if (md5 == null || !md5.equals(getMD5(seckillId))) {
			throw new SeckillException("seckill data rewrite");
		}
		// 执行秒杀逻辑：减库存+记录购买行为
		Date nowTime = new Date();
		try {
		// 减库存
			for(int i=1;i<5;i++){
				Thread.sleep(1000);
				System.out.println("2准备update:"+(i+1)*1000);
			}
		System.out.println("事务1update锁住，事务2查询记录number"+seckillDao.queryById(seckillId).getNumber());
		int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
		System.out.println("事务2update锁住时候number"+seckillDao.queryById(seckillId).getNumber());
		System.out.println("2执行完update语句==================");
		if(updateCount<=0){
			//没有更新，秒杀结束
			throw new SeckillCloseException("seckill is closed");
		}else{
			//记录购买行为
			int insertCount=successKilledDao.insertSuccessKilled(seckillId, userPhone);
			System.out.println("2执行完insert语句==================");
			//唯一seckillId+userPhone,检查重复
			if(insertCount<=0){
				//重复秒杀
				throw new RepeatKillException("seckill repeated");
			}else{
				//秒杀成功
				SuccessKilled successKilled=successKilledDao.queryByIdWithSeckill(seckillId,userPhone);
				System.out.println("2执行完秒杀成功==================");
				return new SeckillExecution(seckillId,SeckillStatEnum.SUCCESS, successKilled);			
			}
		}
		}catch(SeckillCloseException e1){
			throw e1;
		}catch(RepeatKillException e2){
			throw e2;
		} catch (Exception e) {
//			logger.error(e.getMessage(),e);
			//所有编译器异常转化成运行异常
			throw new SeckillException("seckill inner error:"+e.getMessage());
		}
	}
	
}
